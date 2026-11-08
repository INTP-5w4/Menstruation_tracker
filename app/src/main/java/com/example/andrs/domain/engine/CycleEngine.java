package com.example.andrs.domain.engine;

import androidx.annotation.Nullable;

import com.example.andrs.domain.model.Confidence;
import com.example.andrs.domain.model.CyclePhase;
import com.example.andrs.domain.model.CycleSettings;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.model.DayType;
import com.example.andrs.domain.model.Period;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Motor del ciclo menstrual. Java puro (sin dependencias de Android) para poder probarlo en la JVM.
 *
 * <p>Modelo usado (método del calendario, estimación aproximada):
 * <ul>
 *   <li>Duración del ciclo = promedio de los últimos {@link #MAX_CYCLES_FOR_AVERAGE} ciclos válidos,
 *       o el valor configurado si aún no hay ciclos completos.</li>
 *   <li>Ovulación ≈ próxima regla − fase lútea (14 días por defecto).</li>
 *   <li>Ventana fértil = 5 días antes de la ovulación hasta 1 día después.</li>
 *   <li>Fase ovulatoria = ovulación ± 1 día.</li>
 * </ul>
 * No es un método anticonceptivo ni un diagnóstico médico.
 */
public final class CycleEngine {

    public static final int MIN_VALID_CYCLE = 15;
    public static final int MAX_VALID_CYCLE = 60;
    public static final int MAX_VALID_PERIOD = 15;
    public static final int MAX_CYCLES_FOR_AVERAGE = 6;
    /** Una regla sin fecha de fin se considera terminada pasado este número de días. */
    public static final int MAX_OPEN_PERIOD_DAYS = 10;
    public static final int FERTILE_DAYS_BEFORE_OVULATION = 5;
    public static final int FERTILE_DAYS_AFTER_OVULATION = 1;
    /** Diferencia (días) entre ciclo más largo y más corto a partir de la cual se considera irregular. */
    public static final int IRREGULAR_RANGE_DAYS = 7;
    /** Cuántos ciclos futuros se proyectan para el calendario. */
    public static final int PROJECTED_CYCLES = 6;

    public CycleStatus compute(List<Period> periods, CycleSettings settings, LocalDate today) {
        List<Period> sorted = sortedPast(periods, today);
        if (sorted.isEmpty()) {
            return CycleStatus.empty(today, settings);
        }

        List<Integer> cycleLengths = recentCycleLengths(sorted);
        int avgCycle = cycleLengths.isEmpty()
                ? settings.getCycleLength()
                : (int) Math.round(mean(cycleLengths));
        int avgPeriod = averagePeriodLength(sorted, settings.getPeriodLength());
        int luteal = Math.min(settings.getLutealLength(), Math.max(avgCycle - avgPeriod - 1, 1));

        int variability = 0;
        if (cycleLengths.size() >= 2) {
            variability = Collections.max(cycleLengths) - Collections.min(cycleLengths);
        }
        boolean irregular = variability > IRREGULAR_RANGE_DAYS;

        Period last = sorted.get(sorted.size() - 1);
        LocalDate lastStart = last.getStart();
        int cycleDay = (int) ChronoUnit.DAYS.between(lastStart, today) + 1;
        LocalDate lastEnd = effectiveEnd(last, avgPeriod);
        boolean periodOngoing = !today.isAfter(lastEnd);

        LocalDate nextPeriod = lastStart.plusDays(avgCycle);
        int daysUntil = (int) ChronoUnit.DAYS.between(today, nextPeriod);
        boolean late = daysUntil < 0 && !periodOngoing;
        int daysLate = late ? -daysUntil : 0;

        LocalDate ovulation = nextPeriod.minusDays(luteal);
        if (!ovulation.isAfter(lastEnd)) {
            ovulation = lastEnd.plusDays(1);
        }
        LocalDate fertileStart = ovulation.minusDays(FERTILE_DAYS_BEFORE_OVULATION);
        if (!fertileStart.isAfter(lastEnd)) {
            fertileStart = lastEnd.plusDays(1);
        }
        LocalDate fertileEnd = ovulation.plusDays(FERTILE_DAYS_AFTER_OVULATION);

        CyclePhase phase;
        if (periodOngoing) {
            phase = CyclePhase.MENSTRUAL;
        } else if (!today.isBefore(ovulation.minusDays(1)) && !today.isAfter(ovulation.plusDays(1))) {
            phase = CyclePhase.OVULATION;
        } else if (today.isBefore(ovulation)) {
            phase = CyclePhase.FOLLICULAR;
        } else {
            phase = CyclePhase.LUTEAL;
        }

        // Proyecciones para calendario y alertas.
        List<LocalDate> predictedStarts = new ArrayList<>();
        List<LocalDate> predictedOvulations = new ArrayList<>();
        predictedOvulations.add(ovulation);
        LocalDate firstPredicted = late ? today : nextPeriod;
        for (int k = 0; k < PROJECTED_CYCLES; k++) {
            LocalDate start = firstPredicted.plusDays((long) k * avgCycle);
            predictedStarts.add(start);
            predictedOvulations.add(start.plusDays(avgCycle).minusDays(luteal));
        }

        Confidence confidence;
        if (cycleLengths.isEmpty() || irregular) {
            confidence = Confidence.LOW;
        } else if (cycleLengths.size() < 3) {
            confidence = Confidence.MEDIUM;
        } else {
            confidence = Confidence.HIGH;
        }

        return new CycleStatus(true, today, phase, cycleDay, lastStart, nextPeriod, daysUntil, late,
                daysLate, periodOngoing, ovulation, fertileStart, fertileEnd, avgCycle, avgPeriod,
                variability, irregular, cycleLengths.size(), confidence, predictedStarts,
                predictedOvulations);
    }

    /** Clasifica una fecha para el calendario: regla registrada, prevista, ovulación o ventana fértil. */
    public DayType classify(LocalDate date, List<Period> periods, CycleStatus status, CycleSettings settings) {
        if (!status.hasData) {
            return DayType.NONE;
        }
        List<Period> sorted = sortedPast(periods, status.today);
        int avgPeriod = status.averagePeriodLength;

        for (int i = 0; i < sorted.size(); i++) {
            Period p = sorted.get(i);
            LocalDate end = effectiveEnd(p, avgPeriod);
            if (!date.isBefore(p.getStart()) && !date.isAfter(end)) {
                return date.isAfter(status.today) ? DayType.PREDICTED_PERIOD : DayType.PERIOD;
            }
        }

        // Regla prevista (solo desde hoy en adelante).
        if (!date.isBefore(status.today)) {
            for (LocalDate start : status.predictedStarts) {
                LocalDate end = start.plusDays(avgPeriod - 1L);
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    return DayType.PREDICTED_PERIOD;
                }
            }
        }

        // Ovulación / ventana fértil estimadas en ciclos pasados (retrospectivo).
        int luteal = Math.min(settings.getLutealLength(),
                Math.max(status.averageCycleLength - avgPeriod - 1, 1));
        for (int i = 1; i < sorted.size(); i++) {
            LocalDate ov = sorted.get(i).getStart().minusDays(luteal);
            LocalDate prevEnd = effectiveEnd(sorted.get(i - 1), avgPeriod);
            DayType t = fertileType(date, ov, prevEnd);
            if (t != DayType.NONE) return t;
        }
        // Ciclo actual y proyectados.
        LocalDate lastEnd = sorted.isEmpty() ? null
                : effectiveEnd(sorted.get(sorted.size() - 1), avgPeriod);
        for (int i = 0; i < status.predictedOvulations.size(); i++) {
            LocalDate ov = status.predictedOvulations.get(i);
            LocalDate prevEnd = i == 0 ? lastEnd
                    : status.predictedStarts.get(i - 1).plusDays(avgPeriod - 1L);
            DayType t = fertileType(date, ov, prevEnd);
            if (t != DayType.NONE) return t;
        }
        return DayType.NONE;
    }

    /** True si la fecha cae dentro de la ventana fértil estimada (pasada, actual o futura). */
    public boolean isFertile(LocalDate date, List<Period> periods, CycleStatus status, CycleSettings settings) {
        DayType t = classify(date, periods, status, settings);
        return t == DayType.FERTILE || t == DayType.OVULATION;
    }

    // ---------------------------------------------------------------------------------------------

    private static DayType fertileType(LocalDate date, LocalDate ovulation, @Nullable LocalDate prevPeriodEnd) {
        if (date.equals(ovulation)) return DayType.OVULATION;
        LocalDate start = ovulation.minusDays(FERTILE_DAYS_BEFORE_OVULATION);
        LocalDate end = ovulation.plusDays(FERTILE_DAYS_AFTER_OVULATION);
        if (prevPeriodEnd != null && !start.isAfter(prevPeriodEnd)) {
            start = prevPeriodEnd.plusDays(1);
        }
        if (!date.isBefore(start) && !date.isAfter(end)) return DayType.FERTILE;
        return DayType.NONE;
    }

    private static List<Period> sortedPast(List<Period> periods, LocalDate today) {
        List<Period> result = new ArrayList<>();
        for (Period p : periods) {
            if (!p.getStart().isAfter(today)) result.add(p);
        }
        result.sort(Comparator.comparing(Period::getStart));
        return result;
    }

    /** Duraciones de los ciclos más recientes, descartando valores atípicos. */
    static List<Integer> recentCycleLengths(List<Period> sorted) {
        List<Integer> lengths = new ArrayList<>();
        for (int i = sorted.size() - 1; i > 0 && lengths.size() < MAX_CYCLES_FOR_AVERAGE; i--) {
            int len = (int) ChronoUnit.DAYS.between(sorted.get(i - 1).getStart(), sorted.get(i).getStart());
            if (len >= MIN_VALID_CYCLE && len <= MAX_VALID_CYCLE) {
                lengths.add(len);
            }
        }
        return lengths;
    }

    private static int averagePeriodLength(List<Period> sorted, int fallback) {
        List<Integer> lengths = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0 && lengths.size() < MAX_CYCLES_FOR_AVERAGE; i--) {
            Period p = sorted.get(i);
            if (p.getEnd() == null) continue;
            int len = (int) ChronoUnit.DAYS.between(p.getStart(), p.getEnd()) + 1;
            if (len >= 1 && len <= MAX_VALID_PERIOD) lengths.add(len);
        }
        return lengths.isEmpty() ? fallback : (int) Math.round(mean(lengths));
    }

    /**
     * Fin efectivo de una regla. Si no tiene fin registrado se asume la duración promedio
     * (con tope en {@link #MAX_OPEN_PERIOD_DAYS}).
     */
    private static LocalDate effectiveEnd(Period p, int avgPeriod) {
        if (p.getEnd() != null && !p.getEnd().isBefore(p.getStart())) {
            return p.getEnd();
        }
        int days = Math.max(1, Math.min(avgPeriod, MAX_OPEN_PERIOD_DAYS));
        return p.getStart().plusDays(days - 1L);
    }

    private static double mean(List<Integer> values) {
        double sum = 0;
        for (int v : values) sum += v;
        return sum / values.size();
    }
}
