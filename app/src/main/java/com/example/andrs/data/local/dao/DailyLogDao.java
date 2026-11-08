package com.example.andrs.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.andrs.data.local.entity.DailyLogEntity;

import java.util.List;

@Dao
public interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE day = :day")
    LiveData<DailyLogEntity> observe(long day);

    @Query("SELECT * FROM daily_logs WHERE day = :day")
    DailyLogEntity get(long day);

    @Query("SELECT * FROM daily_logs WHERE day BETWEEN :from AND :to ORDER BY day")
    LiveData<List<DailyLogEntity>> observeRange(long from, long to);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DailyLogEntity entity);

    @Query("DELETE FROM daily_logs WHERE day = :day")
    void delete(long day);
}
