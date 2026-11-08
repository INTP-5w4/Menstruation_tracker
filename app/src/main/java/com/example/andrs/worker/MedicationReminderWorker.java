package com.example.andrs.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.andrs.App;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.Medication;

/** Muestra el recordatorio de un medicamento y programa el del día siguiente. */
public class MedicationReminderWorker extends Worker {

    public MedicationReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long id = getInputData().getLong(ReminderScheduler.KEY_MEDICATION_ID, -1);
        if (id < 0) return Result.failure();
        AppContainer c = App.container(getApplicationContext());
        Medication m = c.medications.getSync(id);
        if (m == null || !m.isActive() || !m.isReminderEnabled()) {
            return Result.success();
        }
        NotificationHelper.showMedicationReminder(getApplicationContext(), m.getId(), m.getName(),
                c.settings.isDiscreetNotifications());
        c.reminders.scheduleNextFromWorker(m);
        return Result.success();
    }
}
