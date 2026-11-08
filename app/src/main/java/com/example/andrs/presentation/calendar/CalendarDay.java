package com.example.andrs.presentation.calendar;

import androidx.annotation.Nullable;

import com.example.andrs.domain.model.DayType;

import java.time.LocalDate;

/** Celda del calendario. {@code date} es null para los huecos antes del día 1. */
final class CalendarDay {
    @Nullable final LocalDate date;
    final DayType type;
    final boolean today;
    final boolean hasLog;
    final boolean hasIntimacy;

    CalendarDay(@Nullable LocalDate date, DayType type, boolean today, boolean hasLog, boolean hasIntimacy) {
        this.date = date;
        this.type = type;
        this.today = today;
        this.hasLog = hasLog;
        this.hasIntimacy = hasIntimacy;
    }
}
