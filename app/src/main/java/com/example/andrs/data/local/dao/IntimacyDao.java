package com.example.andrs.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.andrs.data.local.entity.IntimacyEntity;

import java.util.List;

@Dao
public interface IntimacyDao {
    @Query("SELECT * FROM intimacy_logs WHERE day = :day ORDER BY id")
    LiveData<List<IntimacyEntity>> observeForDay(long day);

    @Query("SELECT * FROM intimacy_logs WHERE day BETWEEN :from AND :to ORDER BY day")
    LiveData<List<IntimacyEntity>> observeRange(long from, long to);

    @Insert
    long insert(IntimacyEntity entity);

    @Query("DELETE FROM intimacy_logs WHERE id = :id")
    void delete(long id);
}
