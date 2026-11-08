package com.example.andrs.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.repository.BackupException;

/** Respaldo automático cifrado a la carpeta oculta de Google Drive. */
public class DriveBackupWorker extends Worker {

    public DriveBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppContainer c = App.container(getApplicationContext());
        if (!c.settings.isAutoBackupEnabled() || !c.settings.isDriveLinked()) {
            return Result.success();
        }
        try {
            c.backup.backupToDrive();
            return Result.success();
        } catch (BackupException e) {
            c.settings.setLastBackupError(e.getMessage());
            switch (e.getReason()) {
                case NETWORK:
                    return getRunAttemptCount() < 3 ? Result.retry() : Result.failure();
                case NEEDS_AUTHORIZATION:
                    NotificationHelper.showBackupProblem(getApplicationContext(),
                            getApplicationContext().getString(R.string.notif_backup_reauth));
                    return Result.failure();
                case NO_PASSPHRASE:
                    NotificationHelper.showBackupProblem(getApplicationContext(),
                            getApplicationContext().getString(R.string.notif_backup_no_passphrase));
                    return Result.failure();
                default:
                    return Result.failure();
            }
        }
    }
}
