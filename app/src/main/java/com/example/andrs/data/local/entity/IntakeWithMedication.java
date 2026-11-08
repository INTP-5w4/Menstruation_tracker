package com.example.andrs.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

/** Proyección de una toma junto con datos de su medicamento. */
public class IntakeWithMedication {
    public long id;

    @ColumnInfo(name = "medication_id")
    public long medicationId;

    @ColumnInfo(name = "taken_at")
    public long takenAt;

    @Nullable
    public String notes;

    @ColumnInfo(name = "med_name")
    public String medicationName;

    @ColumnInfo(name = "alters_cycle")
    public boolean altersCycle;
}
