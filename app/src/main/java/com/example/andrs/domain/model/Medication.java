package com.example.andrs.domain.model;

import androidx.annotation.Nullable;

/** Medicamento que el usuario toma o ha tomado. */
public final class Medication {
    private final long id;
    private final String name;
    @Nullable private final String dosage;
    private final MedicationCategory category;
    private final boolean altersCycle;
    private final boolean reminderEnabled;
    private final int reminderHour;
    private final int reminderMinute;
    private final boolean active;
    @Nullable private final String notes;

    public Medication(long id, String name, @Nullable String dosage, MedicationCategory category,
                      boolean altersCycle, boolean reminderEnabled, int reminderHour, int reminderMinute,
                      boolean active, @Nullable String notes) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.category = category;
        this.altersCycle = altersCycle;
        this.reminderEnabled = reminderEnabled;
        this.reminderHour = reminderHour;
        this.reminderMinute = reminderMinute;
        this.active = active;
        this.notes = notes;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    @Nullable public String getDosage() { return dosage; }
    public MedicationCategory getCategory() { return category; }
    public boolean altersCycle() { return altersCycle; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public int getReminderHour() { return reminderHour; }
    public int getReminderMinute() { return reminderMinute; }
    public boolean isActive() { return active; }
    @Nullable public String getNotes() { return notes; }

    public Medication withId(long newId) {
        return new Medication(newId, name, dosage, category, altersCycle, reminderEnabled,
                reminderHour, reminderMinute, active, notes);
    }
}
