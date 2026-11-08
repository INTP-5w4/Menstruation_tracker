package com.example.andrs.presentation.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;

/** Estado compartido entre pestañas (día seleccionado en el registro diario). */
public class MainViewModel extends ViewModel {
    private final MutableLiveData<LocalDate> logDate = new MutableLiveData<>(LocalDate.now());

    public LiveData<LocalDate> getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        logDate.setValue(date.isAfter(today) ? today : date);
    }
}
