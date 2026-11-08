package com.example.andrs.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.andrs.domain.model.Period;

import java.util.List;

public interface CycleRepository {
    LiveData<List<Period>> observePeriods();
    List<Period> getPeriodsSync();
    long insert(Period period);
    void update(Period period);
    void delete(long periodId);
}
