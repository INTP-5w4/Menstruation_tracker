package com.example.andrs.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.andrs.data.local.dao.PeriodDao;
import com.example.andrs.domain.model.Period;
import com.example.andrs.domain.repository.CycleRepository;

import java.util.List;

public class CycleRepositoryImpl implements CycleRepository {
    private final PeriodDao dao;

    public CycleRepositoryImpl(PeriodDao dao) {
        this.dao = dao;
    }

    @Override
    public LiveData<List<Period>> observePeriods() {
        return Transformations.map(dao.observeAll(), Mappers::periods);
    }

    @Override
    public List<Period> getPeriodsSync() {
        return Mappers.periods(dao.getAll());
    }

    @Override
    public long insert(Period period) {
        return dao.insert(Mappers.toEntity(period));
    }

    @Override
    public void update(Period period) {
        dao.update(Mappers.toEntity(period));
    }

    @Override
    public void delete(long periodId) {
        dao.delete(periodId);
    }
}
