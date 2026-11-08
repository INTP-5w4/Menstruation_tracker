package com.example.andrs.presentation.common;

import androidx.lifecycle.MediatorLiveData;

import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.CycleSettings;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.model.Period;

import java.time.LocalDate;
import java.util.List;

/**
 * Combina reglas registradas + ajustes y recalcula el estado del ciclo cuando cambia cualquiera
 * de ellos (o cuando se llama a {@link #refresh()}, p. ej. al cambiar de día).
 */
public class CycleStatusLiveData extends MediatorLiveData<CycleStatus> {
    private final AppContainer c;
    private List<Period> periods;
    private CycleSettings settings;

    public CycleStatusLiveData(AppContainer container) {
        this.c = container;
        addSource(c.cycles.observePeriods(), p -> {
            periods = p;
            refresh();
        });
        addSource(c.settings.observeCycleSettings(), s -> {
            settings = s;
            refresh();
        });
    }

    public void refresh() {
        if (periods == null) return;
        CycleSettings s = settings != null ? settings : c.settings.getCycleSettings();
        setValue(c.getCycleStatus.execute(periods, s, LocalDate.now()));
    }

    public List<Period> getPeriods() {
        return periods;
    }

    public CycleSettings getSettings() {
        return settings != null ? settings : c.settings.getCycleSettings();
    }
}
