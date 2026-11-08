package com.example.andrs.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.andrs.data.local.entity.IntakeWithMedication;
import com.example.andrs.data.local.entity.MedicationEntity;
import com.example.andrs.data.local.entity.MedicationIntakeEntity;

import java.util.List;

@Dao
public interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY active DESC, name COLLATE NOCASE")
    LiveData<List<MedicationEntity>> observeAll();

    @Query("SELECT * FROM medications ORDER BY active DESC, name COLLATE NOCASE")
    List<MedicationEntity> getAll();

    @Query("SELECT * FROM medications WHERE id = :id")
    MedicationEntity get(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(MedicationEntity entity);

    @Query("DELETE FROM medications WHERE id = :id")
    void delete(long id);

    @Query("SELECT i.id, i.medication_id, i.taken_at, i.notes, m.name AS med_name, m.alters_cycle "
            + "FROM medication_intakes i JOIN medications m ON m.id = i.medication_id "
            + "ORDER BY i.taken_at DESC LIMIT :limit")
    LiveData<List<IntakeWithMedication>> observeRecentIntakes(int limit);

    @Query("SELECT COUNT(*) FROM medication_intakes i JOIN medications m ON m.id = i.medication_id "
            + "WHERE m.alters_cycle = 1 AND i.taken_at >= :since")
    int countCycleAlteringSince(long since);

    @Query("SELECT COUNT(*) FROM medication_intakes i JOIN medications m ON m.id = i.medication_id "
            + "WHERE m.alters_cycle = 1 AND i.taken_at >= :since")
    LiveData<Integer> observeCycleAlteringSince(long since);

    @Insert
    long insertIntake(MedicationIntakeEntity entity);

    @Query("DELETE FROM medication_intakes WHERE id = :id")
    void deleteIntake(long id);
}
