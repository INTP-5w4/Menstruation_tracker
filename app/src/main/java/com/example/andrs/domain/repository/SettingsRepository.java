package com.example.andrs.domain.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.andrs.domain.model.CycleSettings;

/** Preferencias del usuario. La implementación las guarda cifradas en el dispositivo. */
public interface SettingsRepository {
    // Aviso de privacidad
    boolean isPrivacyNoticeAccepted();
    void acceptPrivacyNotice();

    // Ciclo
    CycleSettings getCycleSettings();
    LiveData<CycleSettings> observeCycleSettings();
    void saveCycleSettings(CycleSettings settings);

    // Notificaciones
    boolean isPhaseAlertsEnabled();
    void setPhaseAlertsEnabled(boolean enabled);
    boolean isPeriodAlertsEnabled();
    void setPeriodAlertsEnabled(boolean enabled);
    boolean isDiscreetNotifications();
    void setDiscreetNotifications(boolean enabled);
    @Nullable String getLastAlertKey(String type);
    void setLastAlertKey(String type, String value);

    // Seguridad
    boolean isAppLockEnabled();
    void setAppLockEnabled(boolean enabled);
    boolean isScreenshotBlockEnabled();
    void setScreenshotBlockEnabled(boolean enabled);

    // Respaldo
    boolean isDriveLinked();
    void setDriveLinked(boolean linked);
    boolean isAutoBackupEnabled();
    void setAutoBackupEnabled(boolean enabled);
    @Nullable char[] getBackupPassphrase();
    void setBackupPassphrase(@Nullable char[] passphrase);
    long getLastBackupTime();
    void setLastBackupTime(long millis);
    @Nullable String getLastBackupError();
    void setLastBackupError(@Nullable String error);

    /** Borra todas las preferencias excepto la aceptación del aviso de privacidad. */
    void clearAll();
}
