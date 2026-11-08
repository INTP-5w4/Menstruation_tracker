package com.example.andrs.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "medication_intakes",
        foreignKeys = @ForeignKey(entity = MedicationEntity.class, parentColumns = "id",
                childColumns = "medication_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index("medication_id"), @Index("taken_at")})
public class MedicationIntakeEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "medication_id")
    public long medicationId;

    /** Epoch millis en la zona horaria local. */
    @ColumnInfo(name = "taken_at")
    public long takenAt;

    @Nullable
    public String notes;
}
