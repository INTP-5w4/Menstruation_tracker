package com.example.andrs.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_logs")
public class DailyLogEntity {
    @PrimaryKey
    @ColumnInfo(name = "day")
    public long day;

    @NonNull
    public String flow = "NONE";

    /** Códigos de síntomas separados por coma. */
    @NonNull
    public String symptoms = "";

    @NonNull
    public String mood = "NONE";

    @Nullable
    public String notes;
}
