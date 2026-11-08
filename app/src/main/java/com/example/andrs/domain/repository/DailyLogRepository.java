package com.example.andrs.domain.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.andrs.domain.model.DailyLog;

import java.time.LocalDate;
import java.util.List;

public interface DailyLogRepository {
    /** Emite null si no hay registro para esa fecha. */
    LiveData<DailyLog> observe(LocalDate date);
    @Nullable DailyLog getSync(LocalDate date);
    LiveData<List<DailyLog>> observeRange(LocalDate from, LocalDate to);
    /** Guarda el registro; si está vacío, lo elimina. */
    void save(DailyLog log);
}
