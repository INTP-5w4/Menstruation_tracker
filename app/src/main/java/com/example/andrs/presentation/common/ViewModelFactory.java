package com.example.andrs.presentation.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.andrs.di.AppContainer;
import com.example.andrs.presentation.calendar.CalendarViewModel;
import com.example.andrs.presentation.home.HomeViewModel;
import com.example.andrs.presentation.log.LogViewModel;
import com.example.andrs.presentation.main.MainViewModel;
import com.example.andrs.presentation.meds.MedicationsViewModel;
import com.example.andrs.presentation.settings.SettingsViewModel;

/** Crea los ViewModel inyectando dependencias desde {@link AppContainer}. */
public class ViewModelFactory implements ViewModelProvider.Factory {
    private final AppContainer c;

    public ViewModelFactory(AppContainer container) {
        this.c = container;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> cls) {
        if (cls == MainViewModel.class) return (T) new MainViewModel();
        if (cls == HomeViewModel.class) return (T) new HomeViewModel(c);
        if (cls == CalendarViewModel.class) return (T) new CalendarViewModel(c);
        if (cls == LogViewModel.class) return (T) new LogViewModel(c);
        if (cls == MedicationsViewModel.class) return (T) new MedicationsViewModel(c);
        if (cls == SettingsViewModel.class) return (T) new SettingsViewModel(c);
        throw new IllegalArgumentException("ViewModel desconocido: " + cls.getName());
    }
}
