package com.example.andrs.domain.model;

import androidx.annotation.Nullable;

import java.time.LocalDate;

/** Registro de una relación sexual. */
public final class IntimacyLog {
    private final long id;
    private final LocalDate date;
    private final Protection protection;
    @Nullable private final String notes;

    public IntimacyLog(long id, LocalDate date, Protection protection, @Nullable String notes) {
        this.id = id;
        this.date = date;
        this.protection = protection;
        this.notes = notes;
    }

    public long getId() { return id; }
    public LocalDate getDate() { return date; }
    public Protection getProtection() { return protection; }
    @Nullable public String getNotes() { return notes; }
}
