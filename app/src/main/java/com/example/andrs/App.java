package com.example.andrs;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.andrs.data.security.SecurePrefs;
import com.example.andrs.di.AppContainer;
import com.example.andrs.presentation.lock.AppLockManager;
import com.example.andrs.worker.NotificationHelper;

public class App extends Application {
    private AppContainer container;
    private AppLockManager lockManager;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this, createPreferences());
        lockManager = new AppLockManager(container.settings);
        lockManager.register();
        NotificationHelper.createChannels(this);

        container.executors.diskIO().execute(() -> {
            if (!container.settings.isPrivacyNoticeAccepted()) return;
            container.reminders.ensureCycleAlerts();
            container.reminders.rescheduleAllMedications(container.medications.getAllSync());
            container.reminders.setAutoBackup(
                    container.settings.isAutoBackupEnabled() && container.settings.isDriveLinked());
        });
    }

    /** Preferencias cifradas con el Android Keystore. Las pruebas pueden sustituirlas. */
    protected SharedPreferences createPreferences() {
        return SecurePrefs.create(this);
    }

    public static AppContainer container(Context context) {
        return ((App) context.getApplicationContext()).container;
    }

    public static AppLockManager lockManager(Context context) {
        return ((App) context.getApplicationContext()).lockManager;
    }
}
