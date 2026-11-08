package com.example.andrs.domain.model;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/** Un periodo menstrual registrado. {@code end} es null mientras está en curso. */
public final class Period {
    private final long id;
    private final LocalDate start;
    @Nullable private final LocalDate end;
    @Nullable private final String notes;

    public Period(long id, LocalDate start, @Nullable LocalDate end, @Nullable String notes) {
        this.id = id;
        this.start = Objects.requireNonNull(start);
        this.end = end;
        this.notes = notes;
    }

    public long getId() { return id; }
    public LocalDate getStart() { return start; }
    @Nullable public LocalDate getEnd() { return end; }
    @Nullable public String getNotes() { return notes; }

    public Period withEnd(@Nullable LocalDate newEnd) {
        return new Period(id, start, newEnd, notes);
    }

    public Period withStart(LocalDate newStart) {
        return new Period(id, newStart, end, notes);
    }
}
