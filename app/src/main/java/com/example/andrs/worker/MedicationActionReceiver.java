package com.example.andrs.worker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.di.AppContainer;

import java.time.LocalDateTime;

/** Acción "Ya la tomé" de la notificación: registra la toma sin abrir la app. */
public class MedicationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_TAKEN = "com.example.andrs.action.MEDICATION_TAKEN";
    public static final String EXTRA_MEDICATION_ID = "medication_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_TAKEN.equals(intent.getAction())) return;
        long id = intent.getLongExtra(EXTRA_MEDICATION_ID, -1);
        if (id < 0) return;
        PendingResult pending = goAsync();
        AppContainer c = App.container(context);
        c.executors.diskIO().execute(() -> {
            try {
                if (c.medications.getSync(id) != null) {
                    c.medications.logIntake(id, LocalDateTime.now(), null);
                    c.onDataChanged();
                }
                NotificationHelper.cancel(context, NotificationHelper.medicationNotificationId(id));
                c.executors.mainThread().execute(() -> Toast.makeText(context.getApplicationContext(),
                        R.string.intake_logged, Toast.LENGTH_SHORT).show());
            } finally {
                pending.finish();
            }
        });
    }
}
