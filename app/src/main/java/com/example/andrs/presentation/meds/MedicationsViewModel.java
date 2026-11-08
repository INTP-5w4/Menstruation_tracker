package com.example.andrs.presentation.meds;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.andrs.R;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.Medication;
import com.example.andrs.domain.model.MedicationIntake;
import com.example.andrs.presentation.common.Event;
import com.example.andrs.presentation.common.UiMessage;

import java.time.LocalDateTime;
import java.util.List;

public class MedicationsViewModel extends ViewModel {
    private static final int HISTORY_LIMIT = 100;

    private final AppContainer c;
    private final LiveData<List<Medication>> medications;
    private final LiveData<List<MedicationIntake>> intakes;
    private final MutableLiveData<Event<UiMessage>> messages = new MutableLiveData<>();

    public MedicationsViewModel(AppContainer container) {
        this.c = container;
        medications = c.medications.observeMedications();
        intakes = c.medications.observeRecentIntakes(HISTORY_LIMIT);
    }

    public LiveData<List<Medication>> getMedications() { return medications; }
    public LiveData<List<MedicationIntake>> getIntakes() { return intakes; }
    public LiveData<Event<UiMessage>> getMessages() { return messages; }

    public void save(Medication m) {
        c.executors.diskIO().execute(() -> {
            long id = c.medications.save(m);
            Medication saved = m.getId() == 0 ? m.withId(id) : m;
            c.reminders.scheduleMedication(saved);
            c.onDataChanged();
            messages.postValue(new Event<>(new UiMessage(R.string.saved)));
        });
    }

    public void delete(Medication m) {
        c.executors.diskIO().execute(() -> {
            c.reminders.cancelMedication(m.getId());
            c.medications.delete(m.getId());
            c.onDataChanged();
        });
    }

    public void takeNow(Medication m) {
        c.executors.diskIO().execute(() -> {
            c.medications.logIntake(m.getId(), LocalDateTime.now(), null);
            c.onDataChanged();
            messages.postValue(new Event<>(new UiMessage(R.string.intake_logged)));
        });
    }

    public void deleteIntake(long id) {
        c.executors.diskIO().execute(() -> {
            c.medications.deleteIntake(id);
            c.onDataChanged();
        });
    }
}
