package com.example.andrs.domain.model;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Registro diario de flujo, síntomas, estado de ánimo y notas. */
public final class DailyLog {
    private final LocalDate date;
    private final FlowLevel flow;
    private final Set<Symptom> symptoms;
    private final Mood mood;
    @Nullable private final String notes;

    public DailyLog(LocalDate date, FlowLevel flow, Set<Symptom> symptoms, Mood mood, @Nullable String notes) {
        this.date = date;
        this.flow = flow;
        this.symptoms = symptoms.isEmpty()
                ? Collections.unmodifiableSet(EnumSet.noneOf(Symptom.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(symptoms));
        this.mood = mood;
        this.notes = notes;
    }

    public static DailyLog empty(LocalDate date) {
        return new DailyLog(date, FlowLevel.NONE, EnumSet.noneOf(Symptom.class), Mood.NONE, null);
    }

    public LocalDate getDate() { return date; }
    public FlowLevel getFlow() { return flow; }
    public Set<Symptom> getSymptoms() { return symptoms; }
    public Mood getMood() { return mood; }
    @Nullable public String getNotes() { return notes; }

    public boolean isEmpty() {
        return flow == FlowLevel.NONE && symptoms.isEmpty() && mood == Mood.NONE
                && (notes == null || notes.trim().isEmpty());
    }
}
