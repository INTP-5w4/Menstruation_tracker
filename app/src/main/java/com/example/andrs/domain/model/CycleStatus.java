package com.example.andrs.domain.model;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Resultado del motor del ciclo para una fecha dada ("hoy"). */
public final class CycleStatus {
    public final boolean hasData;
    public final LocalDate today;
    public final CyclePhase phase;
    /** Día del ciclo actual (1 = primer día de la última regla). 0 si no hay datos. */
    public final int cycleDay;
    @Nullable public final LocalDate lastPeriodStart;
    @Nullable public final LocalDate nextPeriodStart;
    /** Negativo si hay retraso. */
    public final int daysUntilNextPeriod;
    public final boolean isLate;
    public final int daysLate;
    public final boolean periodOngoing;
    @Nullable public final LocalDate ovulationDate;
    @Nullable public final LocalDate fertileStart;
    @Nullable public final LocalDate fertileEnd;
    public final int averageCycleLength;
    public final int averagePeriodLength;
    /** Diferencia entre el ciclo más largo y el más corto de los usados en el promedio. */
    public final int variabilityDays;
    public final boolean irregular;
    public final int cyclesUsed;
    public final Confidence confidence;
    /** Inicios de regla previstos (el primero es el próximo). */
    public final List<LocalDate> predictedStarts;
    /** Ovulaciones estimadas: ciclo actual y siguientes. */
    public final List<LocalDate> predictedOvulations;

    public CycleStatus(boolean hasData, LocalDate today, CyclePhase phase, int cycleDay,
                       @Nullable LocalDate lastPeriodStart, @Nullable LocalDate nextPeriodStart,
                       int daysUntilNextPeriod, boolean isLate, int daysLate, boolean periodOngoing,
                       @Nullable LocalDate ovulationDate, @Nullable LocalDate fertileStart,
                       @Nullable LocalDate fertileEnd, int averageCycleLength, int averagePeriodLength,
                       int variabilityDays, boolean irregular, int cyclesUsed, Confidence confidence,
                       List<LocalDate> predictedStarts, List<LocalDate> predictedOvulations) {
        this.hasData = hasData;
        this.today = today;
        this.phase = phase;
        this.cycleDay = cycleDay;
        this.lastPeriodStart = lastPeriodStart;
        this.nextPeriodStart = nextPeriodStart;
        this.daysUntilNextPeriod = daysUntilNextPeriod;
        this.isLate = isLate;
        this.daysLate = daysLate;
        this.periodOngoing = periodOngoing;
        this.ovulationDate = ovulationDate;
        this.fertileStart = fertileStart;
        this.fertileEnd = fertileEnd;
        this.averageCycleLength = averageCycleLength;
        this.averagePeriodLength = averagePeriodLength;
        this.variabilityDays = variabilityDays;
        this.irregular = irregular;
        this.cyclesUsed = cyclesUsed;
        this.confidence = confidence;
        this.predictedStarts = Collections.unmodifiableList(predictedStarts);
        this.predictedOvulations = Collections.unmodifiableList(predictedOvulations);
    }

    public static CycleStatus empty(LocalDate today, CycleSettings settings) {
        return new CycleStatus(false, today, CyclePhase.UNKNOWN, 0, null, null, 0, false, 0, false,
                null, null, null, settings.getCycleLength(), settings.getPeriodLength(), 0, false, 0,
                Confidence.LOW, Collections.emptyList(), Collections.emptyList());
    }
}
