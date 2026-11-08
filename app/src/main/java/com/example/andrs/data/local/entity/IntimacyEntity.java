package com.example.andrs.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "intimacy_logs", indices = {@Index("day")})
public class IntimacyEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "day")
    public long day;

    @NonNull
    public String protection = "NONE";

    @Nullable
    public String notes;
}
