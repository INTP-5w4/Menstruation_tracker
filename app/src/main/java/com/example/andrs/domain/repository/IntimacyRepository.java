package com.example.andrs.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.andrs.domain.model.IntimacyLog;

import java.time.LocalDate;
import java.util.List;

public interface IntimacyRepository {
    LiveData<List<IntimacyLog>> observeForDate(LocalDate date);
    LiveData<List<IntimacyLog>> observeRange(LocalDate from, LocalDate to);
    long insert(IntimacyLog log);
    void delete(long id);
}
