package com.example.andrs.presentation.log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.andrs.R;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.model.DailyLog;
import com.example.andrs.domain.model.DayType;
import com.example.andrs.domain.model.FlowLevel;
import com.example.andrs.domain.model.IntimacyLog;
import com.example.andrs.domain.model.Protection;
import com.example.andrs.domain.usecase.LogPeriodUseCase;
import com.example.andrs.presentation.common.CycleStatusLiveData;
import com.example.andrs.presentation.common.Event;
import com.example.andrs.presentation.common.UiMessage;
import com.example.andrs.presentation.home.HomeViewModel;

import java.time.LocalDate;
import java.util.List;

public class LogViewModel extends ViewModel {
    public enum Signal { SUGGEST_PERIOD_START, FERTILE_UNPROTECTED }

    private final AppContainer c;
    private final MutableLiveData<LocalDate> date = new MutableLiveData<>(LocalDate.now());
    private final LiveData<DailyLog> log;
    private final LiveData<List<IntimacyLog>> intimacy;
    private final CycleStatusLiveData status;
    private final MediatorLiveData<DayType> dayType = new MediatorLiveData<>();
    private final MutableLiveData<Event<UiMessage>> messages = new MutableLiveData<>();
    private final MutableLiveData<Event<Signal>> signals = new MutableLiveData<>();

    public LogViewModel(AppContainer container) {
        this.c = container;
        log = Transformations.switchMap(date, c.dailyLogs::observe);
        intimacy = Transformations.switchMap(date, c.intimacy::observeForDate);
        status = new CycleStatusLiveData(container);
        dayType.addSource(date, d -> computeDayType());
        dayType.addSource(status, s -> computeDayType());
    }

    public LiveData<LocalDate> getDate() { return date; }
    public LiveData<DailyLog> getLog() { return log; }
    public LiveData<List<IntimacyLog>> getIntimacy() { return intimacy; }
    public LiveData<DayType> getDayType() { return dayType; }
    public LiveData<Event<UiMessage>> getMessages() { return messages; }
    public LiveData<Event<Signal>> getSignals() { return signals; }

    public void setDate(LocalDate d) {
        if (!d.equals(date.getValue())) date.setValue(d);
    }

    public void save(DailyLog entry) {
        c.executors.diskIO().execute(() -> {
            c.dailyLogs.save(entry);
            c.onDataChanged();
            boolean realFlow = entry.getFlow() == FlowLevel.LIGHT || entry.getFlow() == FlowLevel.MEDIUM
                    || entry.getFlow() == FlowLevel.HEAVY;
            CycleStatus s = status.getValue();
            boolean inPeriod = s != null && c.engine.classify(entry.getDate(), c.cycles.getPeriodsSync(), s,
                    c.settings.getCycleSettings()) == DayType.PERIOD;
            if (realFlow && !inPeriod) {
                signals.postValue(new Event<>(Signal.SUGGEST_PERIOD_START));
            } else {
                messages.postValue(new Event<>(new UiMessage(R.string.saved)));
            }
        });
    }

    public void markPeriodStart() {
        LocalDate d = date.getValue();
        c.executors.diskIO().execute(() -> {
            LogPeriodUseCase.Result r = c.logPeriod.startPeriod(d, LocalDate.now());
            if (r == LogPeriodUseCase.Result.STARTED) c.onDataChanged();
            messages.postValue(new Event<>(new UiMessage(HomeViewModel.resultMessage(r))));
        });
    }

    public void addIntimacy(Protection protection, @Nullable String notes) {
        LocalDate d = date.getValue();
        DayType t = dayType.getValue();
        c.executors.diskIO().execute(() -> {
            c.intimacy.insert(new IntimacyLog(0, d, protection, notes));
            c.onDataChanged();
            if (protection == Protection.NONE && (t == DayType.FERTILE || t == DayType.OVULATION)) {
                signals.postValue(new Event<>(Signal.FERTILE_UNPROTECTED));
            } else {
                messages.postValue(new Event<>(new UiMessage(R.string.saved)));
            }
        });
    }

    public void deleteIntimacy(long id) {
        c.executors.diskIO().execute(() -> {
            c.intimacy.delete(id);
            c.onDataChanged();
        });
    }

    private void computeDayType() {
        CycleStatus s = status.getValue();
        LocalDate d = date.getValue();
        if (s == null || d == null || status.getPeriods() == null) return;
        dayType.setValue(c.engine.classify(d, status.getPeriods(), s, status.getSettings()));
    }
}
