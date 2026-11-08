package com.example.andrs.presentation.settings;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.andrs.R;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.BackupInfo;
import com.example.andrs.domain.model.CycleSettings;
import com.example.andrs.domain.repository.BackupException;
import com.example.andrs.domain.repository.SettingsRepository;
import com.example.andrs.presentation.common.BackupErrors;
import com.example.andrs.presentation.common.Event;
import com.example.andrs.presentation.common.UiMessage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class SettingsViewModel extends ViewModel {

    /** Instantánea inmutable de los ajustes para la UI. */
    public static final class State {
        public final CycleSettings cycle;
        public final boolean phaseAlerts, periodAlerts, discreet, appLock, blockScreenshots;
        public final boolean driveLinked, hasPassphrase, autoBackup;
        public final long lastBackup;
        public final String lastError;

        State(SettingsRepository s) {
            cycle = s.getCycleSettings();
            phaseAlerts = s.isPhaseAlertsEnabled();
            periodAlerts = s.isPeriodAlertsEnabled();
            discreet = s.isDiscreetNotifications();
            appLock = s.isAppLockEnabled();
            blockScreenshots = s.isScreenshotBlockEnabled();
            driveLinked = s.isDriveLinked();
            char[] p = s.getBackupPassphrase();
            hasPassphrase = p != null;
            if (p != null) Arrays.fill(p, '\0');
            autoBackup = s.isAutoBackupEnabled();
            lastBackup = s.getLastBackupTime();
            lastError = s.getLastBackupError();
        }
    }

    private interface BackupTask {
        void run() throws Exception;
    }

    private final AppContainer c;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Event<UiMessage>> messages = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> restart = new MutableLiveData<>();
    private final MutableLiveData<Event<BackupInfo>> remoteFound = new MutableLiveData<>();

    public SettingsViewModel(AppContainer container) {
        this.c = container;
        reload();
    }

    public LiveData<State> getState() { return state; }
    public LiveData<Boolean> getBusy() { return busy; }
    public LiveData<Event<UiMessage>> getMessages() { return messages; }
    public LiveData<Event<Boolean>> getRestart() { return restart; }
    public LiveData<Event<BackupInfo>> getRemoteFound() { return remoteFound; }

    public void reload() {
        c.executors.diskIO().execute(() -> state.postValue(new State(c.settings)));
    }

    private void update(Runnable r) {
        c.executors.diskIO().execute(() -> {
            r.run();
            state.postValue(new State(c.settings));
        });
    }

    public void saveCycleSettings(CycleSettings s) { update(() -> c.settings.saveCycleSettings(s)); }
    public void setPhaseAlerts(boolean v) { update(() -> c.settings.setPhaseAlertsEnabled(v)); }
    public void setPeriodAlerts(boolean v) { update(() -> c.settings.setPeriodAlertsEnabled(v)); }
    public void setDiscreet(boolean v) { update(() -> c.settings.setDiscreetNotifications(v)); }
    public void setAppLock(boolean v) { update(() -> c.settings.setAppLockEnabled(v)); }
    public void setBlockScreenshots(boolean v) { update(() -> c.settings.setScreenshotBlockEnabled(v)); }

    public void setPassphrase(char[] pass) {
        update(() -> {
            c.settings.setBackupPassphrase(pass);
            Arrays.fill(pass, '\0');
            messages.postValue(new Event<>(new UiMessage(R.string.passphrase_saved)));
        });
    }

    public void setAutoBackup(boolean enabled) {
        update(() -> {
            c.settings.setAutoBackupEnabled(enabled);
            c.reminders.setAutoBackup(enabled && c.settings.isDriveLinked());
        });
    }

    // --- Google Drive ----------------------------------------------------------------------------

    /** Tras autorizar: se marca como conectado y se busca un respaldo existente (cambio de teléfono). */
    public void onDriveAuthorized() {
        runBackup(() -> {
            c.settings.setDriveLinked(true);
            c.reminders.setAutoBackup(c.settings.isAutoBackupEnabled());
            messages.postValue(new Event<>(new UiMessage(R.string.drive_connected)));
            BackupInfo info = c.backup.findDriveBackup();
            if (info != null) remoteFound.postValue(new Event<>(info));
        });
    }

    public void disconnectDrive(boolean deleteRemote) {
        runBackup(() -> {
            if (deleteRemote) {
                try {
                    c.backup.deleteDriveBackup();
                } catch (BackupException ignored) {
                    // Se desconecta igualmente.
                }
            }
            c.driveAuth.revokeBlocking();
            c.settings.setDriveLinked(false);
            c.reminders.setAutoBackup(false);
            messages.postValue(new Event<>(new UiMessage(R.string.drive_disconnected)));
        });
    }

    public void backupNow() {
        runBackup(() -> {
            c.backup.backupToDrive();
            messages.postValue(new Event<>(new UiMessage(R.string.backup_done)));
        });
    }

    public void checkRemoteBackup() {
        runBackup(() -> {
            BackupInfo info = c.backup.findDriveBackup();
            if (info != null) remoteFound.postValue(new Event<>(info));
            else messages.postValue(new Event<>(new UiMessage(R.string.backup_not_found)));
        });
    }

    public void restoreFromDrive(char[] pass) {
        runBackup(() -> {
            try {
                c.backup.restoreFromDrive(pass);
                // Se guarda la frase usada para que los respaldos futuros sigan funcionando.
                c.settings.setBackupPassphrase(pass);
            } finally {
                Arrays.fill(pass, '\0');
            }
            restart.postValue(new Event<>(true));
        });
    }

    // --- Archivo local ---------------------------------------------------------------------------

    public void exportTo(Uri uri, char[] pass) {
        runBackup(() -> {
            try (OutputStream out = c.appContext.getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) throw new BackupException(BackupException.Reason.UNKNOWN, "No se pudo abrir el archivo");
                c.backup.exportEncrypted(out, pass);
            } finally {
                Arrays.fill(pass, '\0');
            }
            messages.postValue(new Event<>(new UiMessage(R.string.backup_exported)));
        });
    }

    public void importFrom(Uri uri, char[] pass) {
        runBackup(() -> {
            try (InputStream in = c.appContext.getContentResolver().openInputStream(uri)) {
                if (in == null) throw new BackupException(BackupException.Reason.INVALID_FILE, "No se pudo abrir el archivo");
                c.backup.importEncrypted(in, pass);
            } finally {
                Arrays.fill(pass, '\0');
            }
            restart.postValue(new Event<>(true));
        });
    }

    /** Frase guardada (para exportar sin volver a pedirla). Quien llama debe limpiarla. */
    public char[] savedPassphrase() {
        return c.settings.getBackupPassphrase();
    }

    // --- Borrado total ---------------------------------------------------------------------------

    public void deleteAll(boolean alsoDrive) {
        runBackup(() -> {
            if (alsoDrive && c.settings.isDriveLinked()) {
                try {
                    c.backup.deleteDriveBackup();
                } catch (BackupException ignored) {
                    // Los datos locales se borran de todas formas.
                }
            }
            c.reminders.cancelAll();
            c.database.clearAllTables();
            c.settings.clearAll();
            c.reminders.ensureCycleAlerts();
            messages.postValue(new Event<>(new UiMessage(R.string.delete_all_done)));
        });
    }

    // ---------------------------------------------------------------------------------------------

    private void runBackup(BackupTask task) {
        busy.setValue(true);
        c.executors.backup().execute(() -> {
            try {
                task.run();
            } catch (BackupException e) {
                messages.postValue(new Event<>(BackupErrors.toMessage(e)));
            } catch (Exception e) {
                messages.postValue(new Event<>(new UiMessage(R.string.err_unknown, String.valueOf(e.getMessage()))));
            } finally {
                busy.postValue(false);
                state.postValue(new State(c.settings));
            }
        });
    }
}
