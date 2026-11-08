package com.example.andrs.domain.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.andrs.domain.model.Confidence;
import com.example.andrs.domain.model.CyclePhase;
import com.example.andrs.domain.model.CycleSettings;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.model.DayType;
import com.example.andrs.domain.model.Period;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CycleEngineTest {
    private final CycleEngine engine = new CycleEngine();
    private final CycleSettings defaults = CycleSettings.defaults();

    private static Period p(String start, String end) {
        return new Period(0, LocalDate.parse(start), end == null ? null : LocalDate.parse(end), null);
    }

    @Test
    public void noData_returnsUnknown() {
        CycleStatus s = engine.compute(Collections.emptyList(), defaults, LocalDate.parse("2026-11-08"));
        assertFalse(s.hasData);
        assertEquals(CyclePhase.UNKNOWN, s.phase);
        assertNull(s.nextPeriodStart);
    }

    @Test
    public void singlePeriod_usesDefaults() {
        List<Period> periods = Collections.singletonList(p("2026-11-01", "2026-11-05"));
        CycleStatus s = engine.compute(periods, defaults, LocalDate.parse("2026-11-03"));
        assertTrue(s.hasData);
        assertEquals(3, s.cycleDay);
        assertEquals(CyclePhase.MENSTRUAL, s.phase);
        assertEquals(LocalDate.parse("2026-11-29"), s.nextPeriodStart);
        assertEquals(LocalDate.parse("2026-11-15"), s.ovulationDate);
        assertEquals(LocalDate.parse("2026-11-10"), s.fertileStart);
        assertEquals(LocalDate.parse("2026-11-16"), s.fertileEnd);
        assertEquals(Confidence.LOW, s.confidence);
        assertEquals(0, s.cyclesUsed);
    }

    @Test
    public void phases_followCalendar() {
        List<Period> periods = Collections.singletonList(p("2026-11-01", "2026-11-05"));
        assertEquals(CyclePhase.FOLLICULAR, engine.compute(periods, defaults, LocalDate.parse("2026-11-08")).phase);
        assertEquals(CyclePhase.OVULATION, engine.compute(periods, defaults, LocalDate.parse("2026-11-14")).phase);
        assertEquals(CyclePhase.OVULATION, engine.compute(periods, defaults, LocalDate.parse("2026-11-16")).phase);
        assertEquals(CyclePhase.LUTEAL, engine.compute(periods, defaults, LocalDate.parse("2026-11-20")).phase);
    }

    @Test
    public void averagesRecentCycles() {
        List<Period> periods = Arrays.asList(
                p("2026-07-01", "2026-07-05"),
                p("2026-07-31", "2026-08-04"), // 30
                p("2026-08-30", "2026-09-03"), // 30
                p("2026-09-29", "2026-10-02")); // 30
        CycleStatus s = engine.compute(periods, defaults, LocalDate.parse("2026-10-10"));
        assertEquals(30, s.averageCycleLength);
        assertEquals(5, s.averagePeriodLength); // (5+5+5+4)/4 = 4.75 -> 5
        assertEquals(LocalDate.parse("2026-10-29"), s.nextPeriodStart);
        assertEquals(3, s.cyclesUsed);
        assertEquals(Confidence.HIGH, s.confidence);
        assertFalse(s.irregular);
    }

    @Test
    public void outlierCyclesAreIgnored() {
        List<Period> periods = Arrays.asList(
                p("2026-01-01", "2026-01-05"),
                p("2026-05-01", "2026-05-05"), // 120 días: se descarta (registro olvidado)
                p("2026-05-29", "2026-06-02")); // 28
        CycleStatus s = engine.compute(periods, defaults, LocalDate.parse("2026-06-05"));
        assertEquals(1, s.cyclesUsed);
        assertEquals(28, s.averageCycleLength);
    }

    @Test
    public void irregularCycles_lowConfidence() {
        List<Period> periods = Arrays.asList(
                p("2026-06-01", null),
                p("2026-06-23", null), // 22
                p("2026-07-31", null), // 38
                p("2026-08-28", null)); // 28
        CycleStatus s = engine.compute(periods, defaults, LocalDate.parse("2026-09-05"));
        assertTrue(s.irregular);
        assertEquals(16, s.variabilityDays);
        assertEquals(Confidence.LOW, s.confidence);
    }

    @Test
    public void latePeriod_isDetected() {
        List<Period> periods = Collections.singletonList(p("2026-10-01", "2026-10-05"));
        CycleStatus s = engine.compute(periods, defaults, LocalDate.parse("2026-11-02"));
        assertTrue(s.isLate);
        assertEquals(4, s.daysLate);
        assertEquals(CyclePhase.LUTEAL, s.phase);
        // Con retraso, la siguiente regla prevista se proyecta desde hoy.
        assertEquals(LocalDate.parse("2026-11-02"), s.predictedStarts.get(0));
    }

    @Test
    public void ongoingPeriodWithoutEnd_isMenstrual() {
        List<Period> periods = Collections.singletonList(p("2026-11-06", null));
        CycleStatus s = engine.compute(periods, defaults, LocalDate.parse("2026-11-08"));
        assertTrue(s.periodOngoing);
        assertEquals(CyclePhase.MENSTRUAL, s.phase);
    }

    @Test
    public void futurePeriodsAreIgnored() {
        List<Period> periods = new ArrayList<>(Arrays.asList(p("2026-11-01", "2026-11-04"), p("2026-12-30", null)));
        CycleStatus s = engine.compute(periods, defaults, LocalDate.parse("2026-11-08"));
        assertEquals(LocalDate.parse("2026-11-01"), s.lastPeriodStart);
    }

    @Test
    public void shortCycle_ovulationAfterPeriod() {
        CycleSettings shortCycle = new CycleSettings(21, 7, 14);
        List<Period> periods = Collections.singletonList(p("2026-11-01", "2026-11-07"));
        CycleStatus s = engine.compute(periods, shortCycle, LocalDate.parse("2026-11-02"));
        assertTrue(s.ovulationDate.isAfter(LocalDate.parse("2026-11-07")));
        assertTrue(s.fertileStart.isAfter(LocalDate.parse("2026-11-07")));
    }

    @Test
    public void classify_marksCalendarDays() {
        List<Period> periods = Collections.singletonList(p("2026-11-01", "2026-11-05"));
        LocalDate today = LocalDate.parse("2026-11-08");
        CycleStatus s = engine.compute(periods, defaults, today);
        assertEquals(DayType.PERIOD, engine.classify(LocalDate.parse("2026-11-03"), periods, s, defaults));
        assertEquals(DayType.NONE, engine.classify(LocalDate.parse("2026-11-07"), periods, s, defaults));
        assertEquals(DayType.FERTILE, engine.classify(LocalDate.parse("2026-11-12"), periods, s, defaults));
        assertEquals(DayType.OVULATION, engine.classify(LocalDate.parse("2026-11-15"), periods, s, defaults));
        assertEquals(DayType.NONE, engine.classify(LocalDate.parse("2026-11-20"), periods, s, defaults));
        assertEquals(DayType.PREDICTED_PERIOD, engine.classify(LocalDate.parse("2026-11-29"), periods, s, defaults));
        assertEquals(DayType.PREDICTED_PERIOD, engine.classify(LocalDate.parse("2026-12-03"), periods, s, defaults));
        assertEquals(DayType.NONE, engine.classify(LocalDate.parse("2026-12-04"), periods, s, defaults));
        // Siguiente ciclo proyectado: ovulación 14 días antes del 27 de diciembre.
        assertEquals(DayType.OVULATION, engine.classify(LocalDate.parse("2026-12-13"), periods, s, defaults));
    }

    @Test
    public void classify_retrospectiveOvulationForPastCycles() {
        List<Period> periods = Arrays.asList(p("2026-09-01", "2026-09-05"), p("2026-09-29", "2026-10-03"));
        CycleStatus s = engine.compute(periods, defaults, LocalDate.parse("2026-10-10"));
        assertEquals(DayType.OVULATION, engine.classify(LocalDate.parse("2026-09-15"), periods, s, defaults));
    }
}
