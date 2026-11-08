package com.example.andrs.domain.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.andrs.domain.model.Medication;
import com.example.andrs.domain.model.MedicationIntake;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicationRepository {
    LiveData<List<Medication>> observeMedications();
    List<Medication> getAllSync();
    @Nullable Medication getSync(long id);
    long save(Medication medication);
    void delete(long id);

    LiveData<List<MedicationIntake>> observeRecentIntakes(int limit);
    /** Tomas de medicamentos que alteran el ciclo desde la fecha indicada. */
    int countCycleAlteringIntakesSince(LocalDateTime since);
    LiveData<Integer> observeCycleAlteringIntakesSince(LocalDateTime since);
    long logIntake(long medicationId, LocalDateTime takenAt, @Nullable String notes);
    void deleteIntake(long intakeId);
}
