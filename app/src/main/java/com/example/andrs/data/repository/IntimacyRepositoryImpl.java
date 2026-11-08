package com.example.andrs.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.andrs.data.local.dao.IntimacyDao;
import com.example.andrs.data.local.entity.IntimacyEntity;
import com.example.andrs.domain.model.IntimacyLog;
import com.example.andrs.domain.repository.IntimacyRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IntimacyRepositoryImpl implements IntimacyRepository {
    private final IntimacyDao dao;

    public IntimacyRepositoryImpl(IntimacyDao dao) {
        this.dao = dao;
    }

    @Override
    public LiveData<List<IntimacyLog>> observeForDate(LocalDate date) {
        return Transformations.map(dao.observeForDay(date.toEpochDay()), IntimacyRepositoryImpl::map);
    }

    @Override
    public LiveData<List<IntimacyLog>> observeRange(LocalDate from, LocalDate to) {
        return Transformations.map(dao.observeRange(from.toEpochDay(), to.toEpochDay()),
                IntimacyRepositoryImpl::map);
    }

    @Override
    public long insert(IntimacyLog log) {
        return dao.insert(Mappers.toEntity(log));
    }

    @Override
    public void delete(long id) {
        dao.delete(id);
    }

    private static List<IntimacyLog> map(@Nullable List<IntimacyEntity> list) {
        List<IntimacyLog> out = new ArrayList<>();
        if (list != null) for (IntimacyEntity e : list) out.add(Mappers.toDomain(e));
        return out;
    }
}
