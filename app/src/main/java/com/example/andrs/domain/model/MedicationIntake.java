package com.example.andrs.domain.model;

import androidx.annotation.Nullable;

import java.time.LocalDateTime;

/** Una toma registrada de un medicamento. */
public final class MedicationIntake {
    private final long id;
    private final long medicationId;
    private final String medicationName;
    private final boolean altersCycle;
    private final LocalDateTime takenAt;
    @Nullable private final String notes;

    public MedicationIntake(long id, long medicationId, String medicationName, boolean altersCycle,
                            LocalDateTime takenAt, @Nullable String notes) {
        this.id = id;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.altersCycle = altersCycle;
        this.takenAt = takenAt;
        this.notes = notes;
    }

    public long getId() { return id; }
    public long getMedicationId() { return medicationId; }
    public String getMedicationName() { return medicationName; }
    public boolean altersCycle() { return altersCycle; }
    public LocalDateTime getTakenAt() { return takenAt; }
    @Nullable public String getNotes() { return notes; }
}
