package com.example.andrs.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.andrs.data.local.dao.MedicationDao;
import com.example.andrs.data.local.entity.IntakeWithMedication;
import com.example.andrs.data.local.entity.MedicationEntity;
import com.example.andrs.data.local.entity.MedicationIntakeEntity;
import com.example.andrs.domain.model.Medication;
import com.example.andrs.domain.model.MedicationIntake;
import com.example.andrs.domain.repository.MedicationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicationRepositoryImpl implements MedicationRepository {
    private final MedicationDao dao;

    public MedicationRepositoryImpl(MedicationDao dao) {
        this.dao = dao;
    }

    @Override
    public LiveData<List<Medication>> observeMedications() {
        return Transformations.map(dao.observeAll(), MedicationRepositoryImpl::map);
    }

    @Override
    public List<Medication> getAllSync() {
        return map(dao.getAll());
    }

    @Nullable
    @Override
    public Medication getSync(long id) {
        return Mappers.toDomain(dao.get(id));
    }

    @Override
    public long save(Medication medication) {
        return dao.upsert(Mappers.toEntity(medication));
    }

    @Override
    public void delete(long id) {
        dao.delete(id);
    }

    @Override
    public LiveData<List<MedicationIntake>> observeRecentIntakes(int limit) {
        return Transformations.map(dao.observeRecentIntakes(limit), list -> {
            List<MedicationIntake> out = new ArrayList<>();
            if (list != null) for (IntakeWithMedication e : list) out.add(Mappers.toDomain(e));
            return out;
        });
    }

    @Override
    public int countCycleAlteringIntakesSince(LocalDateTime since) {
        return dao.countCycleAlteringSince(Mappers.toMillis(since));
    }

    @Override
    public LiveData<Integer> observeCycleAlteringIntakesSince(LocalDateTime since) {
        return dao.observeCycleAlteringSince(Mappers.toMillis(since));
    }

    @Override
    public long logIntake(long medicationId, LocalDateTime takenAt, @Nullable String notes) {
        MedicationIntakeEntity e = new MedicationIntakeEntity();
        e.medicationId = medicationId;
        e.takenAt = Mappers.toMillis(takenAt);
        e.notes = notes;
        return dao.insertIntake(e);
    }

    @Override
    public void deleteIntake(long intakeId) {
        dao.deleteIntake(intakeId);
    }

    private static List<Medication> map(@Nullable List<MedicationEntity> list) {
        List<Medication> out = new ArrayList<>();
        if (list != null) for (MedicationEntity e : list) out.add(Mappers.toDomain(e));
        return out;
    }
}
