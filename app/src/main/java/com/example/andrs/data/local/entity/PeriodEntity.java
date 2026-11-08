package com.example.andrs.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Fechas guardadas como "epoch day" (días desde 1970-01-01). */
@Entity(tableName = "periods", indices = {@Index(value = "start_day", unique = true)})
public class PeriodEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "start_day")
    public long startDay;

    @Nullable
    @ColumnInfo(name = "end_day")
    public Long endDay;

    @Nullable
    public String notes;
}
