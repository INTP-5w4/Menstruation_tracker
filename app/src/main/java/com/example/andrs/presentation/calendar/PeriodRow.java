package com.example.andrs.presentation.calendar;

import com.example.andrs.domain.model.Period;

/** Fila del historial: la regla, su duración y la duración del ciclo que inició (0 si no se sabe). */
final class PeriodRow {
    final Period period;
    final int durationDays;
    final int cycleLength;

    PeriodRow(Period period, int durationDays, int cycleLength) {
        this.period = period;
        this.durationDays = durationDays;
        this.cycleLength = cycleLength;
    }
}
