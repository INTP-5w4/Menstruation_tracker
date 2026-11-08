package com.example.andrs.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "medications")
public class MedicationEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @Nullable
    public String dosage;

    @NonNull
    public String category = "OTHER";

    @ColumnInfo(name = "alters_cycle")
    public boolean altersCycle;

    @ColumnInfo(name = "reminder_enabled")
    public boolean reminderEnabled;

    @ColumnInfo(name = "reminder_hour")
    public int reminderHour;

    @ColumnInfo(name = "reminder_minute")
    public int reminderMinute;

    public boolean active = true;

    @Nullable
    public String notes;
}
