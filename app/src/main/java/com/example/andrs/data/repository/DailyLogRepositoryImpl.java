package com.example.andrs.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.andrs.data.local.dao.DailyLogDao;
import com.example.andrs.data.local.entity.DailyLogEntity;
import com.example.andrs.domain.model.DailyLog;
import com.example.andrs.domain.repository.DailyLogRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyLogRepositoryImpl implements DailyLogRepository {
    private final DailyLogDao dao;

    public DailyLogRepositoryImpl(DailyLogDao dao) {
        this.dao = dao;
    }

    @Override
    public LiveData<DailyLog> observe(LocalDate date) {
        return Transformations.map(dao.observe(date.toEpochDay()), Mappers::toDomain);
    }

    @Nullable
    @Override
    public DailyLog getSync(LocalDate date) {
        return Mappers.toDomain(dao.get(date.toEpochDay()));
    }

    @Override
    public LiveData<List<DailyLog>> observeRange(LocalDate from, LocalDate to) {
        return Transformations.map(dao.observeRange(from.toEpochDay(), to.toEpochDay()), list -> {
            List<DailyLog> out = new ArrayList<>();
            if (list != null) for (DailyLogEntity e : list) out.add(Mappers.toDomain(e));
            return out;
        });
    }

    @Override
    public void save(DailyLog log) {
        if (log.isEmpty()) {
            dao.delete(log.getDate().toEpochDay());
        } else {
            dao.upsert(Mappers.toEntity(log));
        }
    }
}
