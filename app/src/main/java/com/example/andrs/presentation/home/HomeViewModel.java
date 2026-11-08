package com.example.andrs.presentation.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.andrs.R;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.model.DailyLog;
import com.example.andrs.domain.model.Medication;
import com.example.andrs.domain.usecase.LogPeriodUseCase;
import com.example.andrs.presentation.common.CycleStatusLiveData;
import com.example.andrs.presentation.common.Event;
import com.example.andrs.presentation.common.UiMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HomeViewModel extends ViewModel {
    /** Ventana en la que una toma de un medicamento hormonal puede seguir afectando el ciclo. */
    private static final int HORMONAL_LOOKBACK_DAYS = 60;

    private final AppContainer c;
    private final CycleStatusLiveData status;
    private final MediatorLiveData<Boolean> hormonalWarning = new MediatorLiveData<>();
    private final MutableLiveData<Event<UiMessage>> messages = new MutableLiveData<>();
    private final LiveData<DailyLog> todayLog;

    private boolean activeAltering;
    private int recentAlteringIntakes;

    public HomeViewModel(AppContainer container) {
        this.c = container;
        this.status = new CycleStatusLiveData(container);
        this.todayLog = container.dailyLogs.observe(LocalDate.now());

        hormonalWarning.addSource(c.medications.observeMedications(), meds -> {
            activeAltering = false;
            if (meds != null) for (Medication m : meds) {
                if (m.isActive() && m.altersCycle()) activeAltering = true;
            }
            hormonalWarning.setValue(activeAltering || recentAlteringIntakes > 0);
        });
        hormonalWarning.addSource(c.medications.observeCycleAlteringIntakesSince(
                LocalDateTime.now().minusDays(HORMONAL_LOOKBACK_DAYS)), count -> {
            recentAlteringIntakes = count == null ? 0 : count;
            hormonalWarning.setValue(activeAltering || recentAlteringIntakes > 0);
        });
    }

    public LiveData<CycleStatus> getStatus() { return status; }
    public LiveData<Boolean> getHormonalWarning() { return hormonalWarning; }
    public LiveData<Event<UiMessage>> getMessages() { return messages; }
    public LiveData<DailyLog> getTodayLog() { return todayLog; }

    public void refresh() {
        status.refresh();
    }

    public void startPeriod(LocalDate date) {
        c.executors.diskIO().execute(() -> post(c.logPeriod.startPeriod(date, LocalDate.now())));
    }

    public void endPeriod(LocalDate date) {
        c.executors.diskIO().execute(() -> post(c.logPeriod.endPeriod(date, LocalDate.now())));
    }

    private void post(LogPeriodUseCase.Result result) {
        if (result == LogPeriodUseCase.Result.STARTED || result == LogPeriodUseCase.Result.ENDED) {
            c.onDataChanged();
        }
        messages.postValue(new Event<>(new UiMessage(messageFor(result))));
    }

    static int messageFor(LogPeriodUseCase.Result r) {
        switch (r) {
            case STARTED: return R.string.period_started;
            case ENDED: return R.string.period_ended;
            case ALREADY_IN_PERIOD: return R.string.period_already;
            case TOO_CLOSE_TO_PREVIOUS: return R.string.period_too_close;
            case NO_PERIOD_TO_END: return R.string.period_nothing_to_end;
            case END_BEFORE_START: return R.string.period_end_before_start;
            case TOO_LONG: return R.string.period_too_long;
            case FUTURE_DATE: return R.string.period_future;
            default: return R.string.saved;
        }
    }

    /** Para que otros ViewModels reutilicen los textos de resultados. */
    public static int resultMessage(LogPeriodUseCase.Result r) {
        return messageFor(r);
    }

}
