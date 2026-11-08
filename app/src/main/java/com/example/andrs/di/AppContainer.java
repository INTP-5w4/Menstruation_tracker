package com.example.andrs.di;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.andrs.data.local.AppDatabase;
import com.example.andrs.data.repository.BackupRepositoryImpl;
import com.example.andrs.data.repository.CycleRepositoryImpl;
import com.example.andrs.data.repository.DailyLogRepositoryImpl;
import com.example.andrs.data.repository.IntimacyRepositoryImpl;
import com.example.andrs.data.repository.MedicationRepositoryImpl;
import com.example.andrs.data.repository.SettingsRepositoryImpl;
import com.example.andrs.data.security.BackupCrypto;
import com.example.andrs.data.sync.DriveAuthManager;
import com.example.andrs.data.sync.DriveRestClient;
import com.example.andrs.domain.engine.CycleEngine;
import com.example.andrs.domain.repository.BackupRepository;
import com.example.andrs.domain.repository.CycleRepository;
import com.example.andrs.domain.repository.DailyLogRepository;
import com.example.andrs.domain.repository.IntimacyRepository;
import com.example.andrs.domain.repository.MedicationRepository;
import com.example.andrs.domain.repository.SettingsRepository;
import com.example.andrs.domain.usecase.GetCycleStatusUseCase;
import com.example.andrs.domain.usecase.LogPeriodUseCase;
import com.example.andrs.worker.ReminderScheduler;

/** Inyección de dependencias manual (sin frameworks): un único grafo por proceso. */
public final class AppContainer {
    public final Context appContext;
    public final AppExecutors executors = new AppExecutors();
    public final AppDatabase database;

    public final SettingsRepository settings;
    public final CycleRepository cycles;
    public final DailyLogRepository dailyLogs;
    public final MedicationRepository medications;
    public final IntimacyRepository intimacy;
    public final BackupRepository backup;
    public final DriveAuthManager driveAuth;

    public final CycleEngine engine = new CycleEngine();
    public final GetCycleStatusUseCase getCycleStatus;
    public final LogPeriodUseCase logPeriod;
    public final ReminderScheduler reminders;

    /** @param preferences almacenamiento de ajustes (cifrado en producción). */
    public AppContainer(Context context, SharedPreferences preferences) {
        appContext = context.getApplicationContext();
        database = AppDatabase.build(appContext);
        settings = new SettingsRepositoryImpl(preferences);
        cycles = new CycleRepositoryImpl(database.periodDao());
        dailyLogs = new DailyLogRepositoryImpl(database.dailyLogDao());
        medications = new MedicationRepositoryImpl(database.medicationDao());
        intimacy = new IntimacyRepositoryImpl(database.intimacyDao());
        driveAuth = new DriveAuthManager(appContext);
        backup = new BackupRepositoryImpl(appContext, database, settings, driveAuth,
                new DriveRestClient(), new BackupCrypto());
        getCycleStatus = new GetCycleStatusUseCase(engine);
        logPeriod = new LogPeriodUseCase(cycles);
        reminders = new ReminderScheduler(appContext);
    }

    /** Llamar tras cualquier escritura: programa un respaldo a Drive si está activado. */
    public void onDataChanged() {
        if (settings.isAutoBackupEnabled() && settings.isDriveLinked()) {
            reminders.backupSoon();
        }
    }
}
