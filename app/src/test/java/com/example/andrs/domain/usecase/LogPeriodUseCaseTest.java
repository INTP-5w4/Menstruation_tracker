package com.example.andrs.domain.usecase;

import static org.junit.Assert.assertEquals;

import androidx.lifecycle.LiveData;

import com.example.andrs.domain.model.Period;
import com.example.andrs.domain.repository.CycleRepository;
import com.example.andrs.domain.usecase.LogPeriodUseCase.Result;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LogPeriodUseCaseTest {
    private static final LocalDate TODAY = LocalDate.parse("2026-11-08");

    private static class FakeRepo implements CycleRepository {
        final List<Period> periods = new ArrayList<>();
        long nextId = 1;

        @Override public LiveData<List<Period>> observePeriods() { throw new UnsupportedOperationException(); }
        @Override public List<Period> getPeriodsSync() { return new ArrayList<>(periods); }
        @Override public long insert(Period p) {
            periods.add(new Period(nextId, p.getStart(), p.getEnd(), p.getNotes()));
            return nextId++;
        }
        @Override public void update(Period p) {
            periods.replaceAll(old -> old.getId() == p.getId() ? p : old);
        }
        @Override public void delete(long id) { periods.removeIf(p -> p.getId() == id); }
    }

    private final FakeRepo repo = new FakeRepo();
    private final LogPeriodUseCase useCase = new LogPeriodUseCase(repo);

    @Test
    public void startAndEnd() {
        assertEquals(Result.STARTED, useCase.startPeriod(LocalDate.parse("2026-11-03"), TODAY));
        assertEquals(Result.ENDED, useCase.endPeriod(LocalDate.parse("2026-11-07"), TODAY));
        assertEquals(LocalDate.parse("2026-11-07"), repo.periods.get(0).getEnd());
    }

    @Test
    public void rejectsFutureAndDuplicates() {
        assertEquals(Result.FUTURE_DATE, useCase.startPeriod(TODAY.plusDays(1), TODAY));
        useCase.startPeriod(LocalDate.parse("2026-11-03"), TODAY);
        assertEquals(Result.ALREADY_IN_PERIOD, useCase.startPeriod(LocalDate.parse("2026-11-05"), TODAY));
        assertEquals(1, repo.periods.size());
    }

    @Test
    public void rejectsTooLongPeriod() {
        useCase.startPeriod(LocalDate.parse("2026-10-01"), TODAY);
        assertEquals(Result.TOO_LONG, useCase.endPeriod(LocalDate.parse("2026-10-20"), TODAY));
    }

    @Test
    public void endWithoutStart() {
        assertEquals(Result.NO_PERIOD_TO_END, useCase.endPeriod(TODAY, TODAY));
    }
}
