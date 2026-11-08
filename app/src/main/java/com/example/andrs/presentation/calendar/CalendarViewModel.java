package com.example.andrs.presentation.calendar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.model.DailyLog;
import com.example.andrs.domain.model.DayType;
import com.example.andrs.domain.model.IntimacyLog;
import com.example.andrs.domain.model.Period;
import com.example.andrs.domain.usecase.LogPeriodUseCase;
import com.example.andrs.presentation.common.CycleStatusLiveData;
import com.example.andrs.presentation.common.Event;
import com.example.andrs.presentation.common.UiMessage;
import com.example.andrs.presentation.home.HomeViewModel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarViewModel extends ViewModel {
    private final AppContainer c;
    private final CycleStatusLiveData status;
    private final MutableLiveData<YearMonth> month = new MutableLiveData<>(YearMonth.now());
    private final MediatorLiveData<List<CalendarDay>> days = new MediatorLiveData<>();
    private final LiveData<List<PeriodRow>> history;
    private final MutableLiveData<Event<UiMessage>> messages = new MutableLiveData<>();

    private List<DailyLog> logs = new ArrayList<>();
    private List<IntimacyLog> intimacy = new ArrayList<>();

    public CalendarViewModel(AppContainer container) {
        this.c = container;
        this.status = new CycleStatusLiveData(container);

        LiveData<List<DailyLog>> logsLive = Transformations.switchMap(month,
                ym -> c.dailyLogs.observeRange(ym.atDay(1), ym.atEndOfMonth()));
        LiveData<List<IntimacyLog>> intimacyLive = Transformations.switchMap(month,
                ym -> c.intimacy.observeRange(ym.atDay(1), ym.atEndOfMonth()));

        days.addSource(status, s -> rebuild());
        days.addSource(month, m -> rebuild());
        days.addSource(logsLive, l -> {
            logs = l == null ? new ArrayList<>() : l;
            rebuild();
        });
        days.addSource(intimacyLive, l -> {
            intimacy = l == null ? new ArrayList<>() : l;
            rebuild();
        });

        history = Transformations.map(status, s -> buildHistory(status.getPeriods()));
    }

    public LiveData<YearMonth> getMonth() { return month; }
    public LiveData<List<CalendarDay>> getDays() { return days; }
    public LiveData<List<PeriodRow>> getHistory() { return history; }
    public LiveData<CycleStatus> getStatus() { return status; }
    public LiveData<Event<UiMessage>> getMessages() { return messages; }

    public void previousMonth() { month.setValue(month.getValue().minusMonths(1)); }
    public void nextMonth() { month.setValue(month.getValue().plusMonths(1)); }
    public void goToday() { month.setValue(YearMonth.now()); status.refresh(); }

    public void updatePeriod(Period p) {
        c.executors.diskIO().execute(() -> {
            LogPeriodUseCase.Result r = c.logPeriod.updatePeriod(p, LocalDate.now());
            if (r == LogPeriodUseCase.Result.ENDED) {
                c.onDataChanged();
                messages.postValue(new Event<>(new UiMessage(com.example.andrs.R.string.period_updated)));
            } else {
                messages.postValue(new Event<>(new UiMessage(HomeViewModel.resultMessage(r))));
            }
        });
    }

    public void deletePeriod(long id) {
        c.executors.diskIO().execute(() -> {
            c.logPeriod.delete(id);
            c.onDataChanged();
            messages.postValue(new Event<>(new UiMessage(com.example.andrs.R.string.period_deleted)));
        });
    }

    private void rebuild() {
        CycleStatus s = status.getValue();
        YearMonth ym = month.getValue();
        if (s == null || ym == null) return;
        List<Period> periods = status.getPeriods();

        Set<LocalDate> logDays = new HashSet<>();
        for (DailyLog l : logs) if (l != null && !l.isEmpty()) logDays.add(l.getDate());
        Set<LocalDate> intimacyDays = new HashSet<>();
        for (IntimacyLog l : intimacy) intimacyDays.add(l.getDate());

        List<CalendarDay> out = new ArrayList<>();
        int leading = ym.atDay(1).getDayOfWeek().getValue() - 1; // Lunes primero
        for (int i = 0; i < leading; i++) out.add(new CalendarDay(null, DayType.NONE, false, false, false));
        LocalDate today = LocalDate.now();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            DayType type = c.engine.classify(date, periods, s, status.getSettings());
            out.add(new CalendarDay(date, type, date.equals(today), logDays.contains(date),
                    intimacyDays.contains(date)));
        }
        days.setValue(out);
    }

    private static List<PeriodRow> buildHistory(List<Period> periods) {
        List<Period> sorted = new ArrayList<>(periods);
        sorted.sort(Comparator.comparing(Period::getStart));
        List<PeriodRow> rows = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Period p = sorted.get(i);
            int duration = p.getEnd() == null ? 0 : (int) ChronoUnit.DAYS.between(p.getStart(), p.getEnd()) + 1;
            int cycle = i + 1 < sorted.size()
                    ? (int) ChronoUnit.DAYS.between(p.getStart(), sorted.get(i + 1).getStart()) : 0;
            rows.add(0, new PeriodRow(p, duration, cycle));
        }
        return rows;
    }
}
