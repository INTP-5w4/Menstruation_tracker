package com.example.andrs.presentation.lock;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.andrs.domain.repository.SettingsRepository;

/**
 * Bloquea la app al iniciar y cuando vuelve del segundo plano tras un periodo de gracia.
 */
public class AppLockManager implements DefaultLifecycleObserver {
    private static final long GRACE_MS = 30_000;

    private final SettingsRepository settings;
    private boolean locked = true;
    private long backgroundSince = 0;
    private boolean externalFlow = false;

    public AppLockManager(SettingsRepository settings) {
        this.settings = settings;
    }

    public void register() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        backgroundSince = SystemClock.elapsedRealtime();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (backgroundSince > 0 && !externalFlow
                && SystemClock.elapsedRealtime() - backgroundSince > GRACE_MS) {
            locked = true;
        }
        externalFlow = false;
    }

    public boolean shouldLock() {
        return locked && settings.isAppLockEnabled();
    }

    public void unlock() {
        locked = false;
    }

    /** Evita bloquear al volver de un selector de archivos o de la pantalla de Google. */
    public void allowExternalFlow() {
        externalFlow = true;
    }
}
