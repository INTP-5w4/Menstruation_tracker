package com.example.andrs.data.repository;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.andrs.domain.model.CycleSettings;
import com.example.andrs.domain.repository.SettingsRepository;

/** Preferencias respaldadas por {@link com.example.andrs.data.security.SecurePrefs}. */
public class SettingsRepositoryImpl implements SettingsRepository {
    /** Si el texto del aviso cambia de forma relevante, subir este número para volver a pedirlo. */
    public static final int PRIVACY_NOTICE_VERSION = 1;

    private static final String K_PRIVACY = "privacy_notice_version";
    private static final String K_CYCLE_LEN = "cycle_length";
    private static final String K_PERIOD_LEN = "period_length";
    private static final String K_LUTEAL_LEN = "luteal_length";
    private static final String K_PHASE_ALERTS = "phase_alerts";
    private static final String K_PERIOD_ALERTS = "period_alerts";
    private static final String K_DISCREET = "discreet_notifications";
    private static final String K_LAST_ALERT = "last_alert_";
    private static final String K_APP_LOCK = "app_lock";
    private static final String K_NO_SCREENSHOTS = "block_screenshots";
    private static final String K_DRIVE_LINKED = "drive_linked";
    private static final String K_AUTO_BACKUP = "auto_backup";
    private static final String K_PASSPHRASE = "backup_passphrase";
    private static final String K_LAST_BACKUP = "last_backup_time";
    private static final String K_LAST_BACKUP_ERROR = "last_backup_error";

    private final SharedPreferences prefs;
    private final MutableLiveData<CycleSettings> cycleSettings = new MutableLiveData<>();

    public SettingsRepositoryImpl(SharedPreferences prefs) {
        this.prefs = prefs;
        cycleSettings.postValue(getCycleSettings());
    }

    @Override
    public boolean isPrivacyNoticeAccepted() {
        return prefs.getInt(K_PRIVACY, 0) >= PRIVACY_NOTICE_VERSION;
    }

    @Override
    public void acceptPrivacyNotice() {
        prefs.edit().putInt(K_PRIVACY, PRIVACY_NOTICE_VERSION).commit();
    }

    @Override
    public CycleSettings getCycleSettings() {
        return new CycleSettings(
                prefs.getInt(K_CYCLE_LEN, CycleSettings.DEFAULT_CYCLE_LENGTH),
                prefs.getInt(K_PERIOD_LEN, CycleSettings.DEFAULT_PERIOD_LENGTH),
                prefs.getInt(K_LUTEAL_LEN, CycleSettings.DEFAULT_LUTEAL_LENGTH));
    }

    @Override
    public LiveData<CycleSettings> observeCycleSettings() {
        return cycleSettings;
    }

    @Override
    public void saveCycleSettings(CycleSettings s) {
        prefs.edit()
                .putInt(K_CYCLE_LEN, s.getCycleLength())
                .putInt(K_PERIOD_LEN, s.getPeriodLength())
                .putInt(K_LUTEAL_LEN, s.getLutealLength())
                .apply();
        cycleSettings.postValue(s);
    }

    @Override public boolean isPhaseAlertsEnabled() { return prefs.getBoolean(K_PHASE_ALERTS, true); }
    @Override public void setPhaseAlertsEnabled(boolean e) { prefs.edit().putBoolean(K_PHASE_ALERTS, e).apply(); }
    @Override public boolean isPeriodAlertsEnabled() { return prefs.getBoolean(K_PERIOD_ALERTS, true); }
    @Override public void setPeriodAlertsEnabled(boolean e) { prefs.edit().putBoolean(K_PERIOD_ALERTS, e).apply(); }
    @Override public boolean isDiscreetNotifications() { return prefs.getBoolean(K_DISCREET, true); }
    @Override public void setDiscreetNotifications(boolean e) { prefs.edit().putBoolean(K_DISCREET, e).apply(); }

    @Nullable
    @Override
    public String getLastAlertKey(String type) {
        return prefs.getString(K_LAST_ALERT + type, null);
    }

    @Override
    public void setLastAlertKey(String type, String value) {
        prefs.edit().putString(K_LAST_ALERT + type, value).apply();
    }

    @Override public boolean isAppLockEnabled() { return prefs.getBoolean(K_APP_LOCK, false); }
    @Override public void setAppLockEnabled(boolean e) { prefs.edit().putBoolean(K_APP_LOCK, e).apply(); }
    @Override public boolean isScreenshotBlockEnabled() { return prefs.getBoolean(K_NO_SCREENSHOTS, true); }
    @Override public void setScreenshotBlockEnabled(boolean e) { prefs.edit().putBoolean(K_NO_SCREENSHOTS, e).apply(); }

    @Override public boolean isDriveLinked() { return prefs.getBoolean(K_DRIVE_LINKED, false); }
    @Override public void setDriveLinked(boolean l) { prefs.edit().putBoolean(K_DRIVE_LINKED, l).apply(); }
    @Override public boolean isAutoBackupEnabled() { return prefs.getBoolean(K_AUTO_BACKUP, false); }
    @Override public void setAutoBackupEnabled(boolean e) { prefs.edit().putBoolean(K_AUTO_BACKUP, e).apply(); }

    @Nullable
    @Override
    public char[] getBackupPassphrase() {
        String s = prefs.getString(K_PASSPHRASE, null);
        return s == null ? null : s.toCharArray();
    }

    @Override
    public void setBackupPassphrase(@Nullable char[] passphrase) {
        if (passphrase == null) {
            prefs.edit().remove(K_PASSPHRASE).apply();
        } else {
            prefs.edit().putString(K_PASSPHRASE, new String(passphrase)).apply();
        }
    }

    @Override public long getLastBackupTime() { return prefs.getLong(K_LAST_BACKUP, 0L); }
    @Override public void setLastBackupTime(long m) { prefs.edit().putLong(K_LAST_BACKUP, m).apply(); }

    @Nullable
    @Override
    public String getLastBackupError() {
        return prefs.getString(K_LAST_BACKUP_ERROR, null);
    }

    @Override
    public void setLastBackupError(@Nullable String error) {
        if (error == null) prefs.edit().remove(K_LAST_BACKUP_ERROR).apply();
        else prefs.edit().putString(K_LAST_BACKUP_ERROR, error).apply();
    }

    @Override
    public void clearAll() {
        int privacy = prefs.getInt(K_PRIVACY, 0);
        prefs.edit().clear().putInt(K_PRIVACY, privacy).commit();
        cycleSettings.postValue(getCycleSettings());
    }
}
