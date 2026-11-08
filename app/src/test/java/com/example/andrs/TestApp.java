package com.example.andrs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

/** App de pruebas: preferencias sin Keystore (Robolectric no lo emula) y WorkManager de prueba. */
public class TestApp extends App {
    @Override
    public void onCreate() {
        WorkManagerTestInitHelper.initializeTestWorkManager(this, new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build());
        super.onCreate();
    }

    @Override
    protected SharedPreferences createPreferences() {
        return getSharedPreferences("test_prefs", Context.MODE_PRIVATE);
    }
}
