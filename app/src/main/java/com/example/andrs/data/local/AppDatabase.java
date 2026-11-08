package com.example.andrs.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.andrs.data.local.dao.DailyLogDao;
import com.example.andrs.data.local.dao.IntimacyDao;
import com.example.andrs.data.local.dao.MedicationDao;
import com.example.andrs.data.local.dao.PeriodDao;
import com.example.andrs.data.local.entity.DailyLogEntity;
import com.example.andrs.data.local.entity.IntimacyEntity;
import com.example.andrs.data.local.entity.MedicationEntity;
import com.example.andrs.data.local.entity.MedicationIntakeEntity;
import com.example.andrs.data.local.entity.PeriodEntity;

@Database(
        entities = {
                PeriodEntity.class,
                DailyLogEntity.class,
                MedicationEntity.class,
                MedicationIntakeEntity.class,
                IntimacyEntity.class
        },
        version = AppDatabase.VERSION,
        exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    public static final int VERSION = 1;
    public static final String NAME = "cycle_tracker.db";

    public abstract PeriodDao periodDao();
    public abstract DailyLogDao dailyLogDao();
    public abstract MedicationDao medicationDao();
    public abstract IntimacyDao intimacyDao();

    public static AppDatabase build(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, NAME)
                .build();
    }
}
