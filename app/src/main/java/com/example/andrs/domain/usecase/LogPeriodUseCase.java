package com.example.andrs.domain.usecase;

import androidx.annotation.Nullable;

import com.example.andrs.domain.engine.CycleEngine;
import com.example.andrs.domain.model.Period;
import com.example.andrs.domain.repository.CycleRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Reglas de negocio para registrar el inicio y el fin de la regla. */
public class LogPeriodUseCase {

    public enum Result {
        STARTED,
        ENDED,
        ALREADY_IN_PERIOD,
        TOO_CLOSE_TO_PREVIOUS,
        NO_PERIOD_TO_END,
        END_BEFORE_START,
        TOO_LONG,
        FUTURE_DATE
    }

    private final CycleRepository repository;

    public LogPeriodUseCase(CycleRepository repository) {
        this.repository = repository;
    }

    public Result startPeriod(LocalDate date, LocalDate today) {
        if (date.isAfter(today)) return Result.FUTURE_DATE;
        List<Period> periods = repository.getPeriodsSync();
        for (Period p : periods) {
            LocalDate end = p.getEnd() != null ? p.getEnd()
                    : p.getStart().plusDays(CycleEngine.MAX_OPEN_PERIOD_DAYS - 1L);
            if (!date.isBefore(p.getStart()) && !date.isAfter(end)) {
                return Result.ALREADY_IN_PERIOD;
            }
            long gap = Math.abs(ChronoUnit.DAYS.between(p.getStart(), date));
            if (gap < CycleEngine.MIN_VALID_CYCLE / 2) {
                return Result.TOO_CLOSE_TO_PREVIOUS;
            }
        }
        repository.insert(new Period(0, date, null, null));
        return Result.STARTED;
    }

    public Result endPeriod(LocalDate date, LocalDate today) {
        if (date.isAfter(today)) return Result.FUTURE_DATE;
        Period target = latestStartingOnOrBefore(repository.getPeriodsSync(), date);
        if (target == null) return Result.NO_PERIOD_TO_END;
        if (date.isBefore(target.getStart())) return Result.END_BEFORE_START;
        long len = ChronoUnit.DAYS.between(target.getStart(), date) + 1;
        if (len > CycleEngine.MAX_VALID_PERIOD) return Result.TOO_LONG;
        repository.update(target.withEnd(date));
        return Result.ENDED;
    }

    /** Edición manual desde el historial. */
    public Result updatePeriod(Period period, LocalDate today) {
        if (period.getStart().isAfter(today)) return Result.FUTURE_DATE;
        if (period.getEnd() != null) {
            if (period.getEnd().isBefore(period.getStart())) return Result.END_BEFORE_START;
            long len = ChronoUnit.DAYS.between(period.getStart(), period.getEnd()) + 1;
            if (len > CycleEngine.MAX_VALID_PERIOD) return Result.TOO_LONG;
        }
        repository.update(period);
        return Result.ENDED;
    }

    public void delete(long periodId) {
        repository.delete(periodId);
    }

    @Nullable
    private static Period latestStartingOnOrBefore(List<Period> periods, LocalDate date) {
        Period best = null;
        for (Period p : periods) {
            if (p.getStart().isAfter(date)) continue;
            if (best == null || p.getStart().isAfter(best.getStart())) best = p;
        }
        return best;
    }
}
