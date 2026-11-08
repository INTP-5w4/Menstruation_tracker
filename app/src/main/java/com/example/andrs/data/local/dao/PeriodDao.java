package com.example.andrs.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.andrs.data.local.entity.PeriodEntity;

import java.util.List;

@Dao
public interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY start_day DESC")
    LiveData<List<PeriodEntity>> observeAll();

    @Query("SELECT * FROM periods ORDER BY start_day DESC")
    List<PeriodEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(PeriodEntity entity);

    @Update
    void update(PeriodEntity entity);

    @Query("DELETE FROM periods WHERE id = :id")
    void delete(long id);
}
