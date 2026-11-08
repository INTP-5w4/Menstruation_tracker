package com.example.andrs.worker;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.andrs.domain.model.Medication;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Programa todas las tareas de WorkManager de la app. */
public class ReminderScheduler {
    static final String KEY_MEDICATION_ID = "medication_id";
    private static final String MED_PREFIX = "med_reminder_";
    private static final String CYCLE_ALERTS = "cycle_alerts_daily";
    private static final String AUTO_BACKUP = "drive_auto_backup";
    private static final String BACKUP_AFTER_CHANGE = "drive_backup_after_change";
    private static final LocalTime CYCLE_ALERT_TIME = LocalTime.of(9, 0);

    private final WorkManager workManager;

    public ReminderScheduler(Context context) {
        this.workManager = WorkManager.getInstance(context.getApplicationContext());
    }

    // --- Medicamentos ----------------------------------------------------------------------------

    /** Llamar al crear/editar un medicamento: reemplaza el recordatorio pendiente. */
    public void scheduleMedication(Medication m) {
        if (!m.isActive() || !m.isReminderEnabled()) {
            cancelMedication(m.getId());
            return;
        }
        enqueueMedication(m, ExistingWorkPolicy.REPLACE);
    }

    /** Desde el propio worker: encadena el siguiente aviso sin cancelar el que se está ejecutando. */
    void scheduleNextFromWorker(Medication m) {
        if (m.isActive() && m.isReminderEnabled()) {
            enqueueMedication(m, ExistingWorkPolicy.APPEND_OR_REPLACE);
        }
    }

    public void rescheduleAllMedications(List<Medication> medications) {
        for (Medication m : medications) {
            if (m.isActive() && m.isReminderEnabled()) {
                enqueueMedication(m, ExistingWorkPolicy.KEEP);
            } else {
                cancelMedication(m.getId());
            }
        }
    }

    public void cancelMedication(long medicationId) {
        workManager.cancelUniqueWork(MED_PREFIX + medicationId);
    }

    private void enqueueMedication(Medication m, ExistingWorkPolicy policy) {
        long delay = millisUntil(LocalTime.of(m.getReminderHour(), m.getReminderMinute()));
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MedicationReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder().putLong(KEY_MEDICATION_ID, m.getId()).build())
                .addTag("medication_reminder")
                .build();
        workManager.enqueueUniqueWork(MED_PREFIX + m.getId(), policy, request);
    }

    // --- Alertas del ciclo -----------------------------------------------------------------------

    public void ensureCycleAlerts() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(CycleAlertWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(millisUntil(CYCLE_ALERT_TIME), TimeUnit.MILLISECONDS)
                .build();
        workManager.enqueueUniquePeriodicWork(CYCLE_ALERTS, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    // --- Respaldo --------------------------------------------------------------------------------

    public void setAutoBackup(boolean enabled) {
        if (!enabled) {
            workManager.cancelUniqueWork(AUTO_BACKUP);
            return;
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(DriveBackupWorker.class, 1, TimeUnit.DAYS)
                .setConstraints(backupConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();
        workManager.enqueueUniquePeriodicWork(AUTO_BACKUP, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /** Respaldo diferido tras cambios: cada nuevo cambio reinicia la espera de 15 min. */
    public void backupSoon() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DriveBackupWorker.class)
                .setConstraints(backupConstraints())
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build();
        workManager.enqueueUniqueWork(BACKUP_AFTER_CHANGE, ExistingWorkPolicy.REPLACE, request);
    }

    public void cancelAll() {
        workManager.cancelAllWork();
    }

    private static Constraints backupConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();
    }

    /** Milisegundos hasta la próxima ocurrencia de {@code time} (al menos 1 minuto en el futuro). */
    static long millisUntil(LocalTime time) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().atTime(time);
        if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1);
        return Duration.between(now, next).toMillis();
    }
}
