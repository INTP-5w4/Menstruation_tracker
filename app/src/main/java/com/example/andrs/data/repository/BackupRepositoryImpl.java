package com.example.andrs.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.andrs.data.local.AppDatabase;
import com.example.andrs.data.security.BackupCrypto;
import com.example.andrs.data.sync.DriveAuthManager;
import com.example.andrs.data.sync.DriveRestClient;
import com.example.andrs.domain.model.BackupInfo;
import com.example.andrs.domain.repository.BackupException;
import com.example.andrs.domain.repository.BackupException.Reason;
import com.example.andrs.domain.repository.BackupRepository;
import com.example.andrs.domain.repository.SettingsRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Respaldo del archivo SQLite completo: se toma una instantánea consistente, se cifra con
 * AES-256-GCM (clave derivada de la frase del usuario) y se sube a la carpeta oculta
 * {@code appDataFolder} de Google Drive, o se exporta a un archivo elegido por el usuario.
 */
public class BackupRepositoryImpl implements BackupRepository {
    private static final byte[] SQLITE_HEADER = "SQLite format 3\u0000".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_IMPORT_BYTES = 64 * 1024 * 1024;

    private final Context context;
    private final AppDatabase database;
    private final SettingsRepository settings;
    private final DriveAuthManager auth;
    private final DriveRestClient drive;
    private final BackupCrypto crypto;

    public BackupRepositoryImpl(Context context, AppDatabase database, SettingsRepository settings,
                                DriveAuthManager auth, DriveRestClient drive, BackupCrypto crypto) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.settings = settings;
        this.auth = auth;
        this.drive = drive;
        this.crypto = crypto;
    }

    private interface DriveCall<T> {
        T run(String token) throws IOException;
    }

    // --- Google Drive ----------------------------------------------------------------------------

    @Override
    public synchronized void backupToDrive() throws BackupException {
        char[] pass = settings.getBackupPassphrase();
        if (pass == null) throw new BackupException(Reason.NO_PASSPHRASE, "Falta la frase de respaldo");
        byte[] encrypted;
        try {
            encrypted = crypto.encrypt(snapshot(), pass);
        } catch (GeneralSecurityException e) {
            throw new BackupException(Reason.UNKNOWN, "No se pudo cifrar el respaldo", e);
        } finally {
            Arrays.fill(pass, '\0');
        }
        withToken(token -> {
            BackupInfo existing = drive.findBackup(token);
            return drive.upload(token, existing == null ? null : existing.remoteId, encrypted);
        });
        settings.setLastBackupTime(System.currentTimeMillis());
        settings.setLastBackupError(null);
    }

    @Nullable
    @Override
    public BackupInfo findDriveBackup() throws BackupException {
        return withToken(drive::findBackup);
    }

    @Override
    public synchronized void restoreFromDrive(char[] passphrase) throws BackupException {
        byte[] encrypted = withToken(token -> {
            BackupInfo info = drive.findBackup(token);
            if (info == null) return null;
            return drive.download(token, info.remoteId);
        });
        if (encrypted == null) throw new BackupException(Reason.NOT_FOUND, "No hay respaldo en Drive");
        replaceDatabase(decrypt(encrypted, passphrase));
    }

    @Override
    public void deleteDriveBackup() throws BackupException {
        withToken(token -> {
            BackupInfo info = drive.findBackup(token);
            if (info != null) drive.delete(token, info.remoteId);
            return null;
        });
    }

    // --- Archivo local ---------------------------------------------------------------------------

    @Override
    public synchronized void exportEncrypted(OutputStream out, char[] passphrase) throws BackupException {
        try {
            out.write(crypto.encrypt(snapshot(), passphrase));
            out.flush();
        } catch (GeneralSecurityException e) {
            throw new BackupException(Reason.UNKNOWN, "No se pudo cifrar el respaldo", e);
        } catch (IOException e) {
            throw new BackupException(Reason.UNKNOWN, "No se pudo escribir el archivo", e);
        }
    }

    @Override
    public synchronized void importEncrypted(InputStream in, char[] passphrase) throws BackupException {
        byte[] data;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[16 * 1024];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
                if (buf.size() > MAX_IMPORT_BYTES) {
                    throw new BackupException(Reason.INVALID_FILE, "Archivo demasiado grande");
                }
            }
            data = buf.toByteArray();
        } catch (IOException e) {
            throw new BackupException(Reason.INVALID_FILE, "No se pudo leer el archivo", e);
        }
        replaceDatabase(decrypt(data, passphrase));
    }

    // --- Internos --------------------------------------------------------------------------------

    private <T> T withToken(DriveCall<T> call) throws BackupException {
        try {
            String token = auth.getAccessTokenBlocking();
            try {
                return call.run(token);
            } catch (DriveRestClient.HttpException e) {
                if (e.code != 401) throw e;
                // Token caducado o revocado: se limpia y se reintenta una vez.
                auth.clearTokenBlocking(token);
                return call.run(auth.getAccessTokenBlocking());
            }
        } catch (DriveAuthManager.ConsentRequiredException e) {
            settings.setDriveLinked(false);
            throw new BackupException(Reason.NEEDS_AUTHORIZATION, "Autoriza Google Drive de nuevo", e);
        } catch (DriveRestClient.HttpException e) {
            if (e.code == 401 || e.code == 403) {
                throw new BackupException(Reason.NEEDS_AUTHORIZATION, "Acceso a Drive denegado", e);
            }
            throw new BackupException(Reason.NETWORK, "Error de Drive (" + e.code + ")", e);
        } catch (IOException e) {
            throw new BackupException(Reason.NETWORK, "Sin conexión con Google Drive", e);
        } catch (Exception e) {
            throw new BackupException(Reason.UNKNOWN, "Error inesperado con Google Drive", e);
        }
    }

    private byte[] decrypt(byte[] encrypted, char[] passphrase) throws BackupException {
        try {
            return crypto.decrypt(encrypted, passphrase);
        } catch (BackupCrypto.WrongPassphraseException e) {
            throw new BackupException(Reason.WRONG_PASSPHRASE, "Frase de respaldo incorrecta", e);
        } catch (BackupCrypto.InvalidFormatException e) {
            throw new BackupException(Reason.INVALID_FILE, e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new BackupException(Reason.UNKNOWN, "No se pudo descifrar el respaldo", e);
        }
    }

    /** Copia consistente de la base de datos actual como bytes. */
    private byte[] snapshot() throws BackupException {
        SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
        File tmp = new File(context.getCacheDir(), "snapshot.db");
        try {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // VACUUM INTO produce una copia atómica y compacta (SQLite 3.27+).
                db.execSQL("VACUUM INTO '" + tmp.getAbsolutePath().replace("'", "''") + "'");
                return Files.readAllBytes(tmp.toPath());
            }
            try (Cursor c = db.query("PRAGMA wal_checkpoint(TRUNCATE)")) {
                c.moveToFirst();
            }
            return Files.readAllBytes(context.getDatabasePath(AppDatabase.NAME).toPath());
        } catch (IOException | RuntimeException e) {
            throw new BackupException(Reason.UNKNOWN, "No se pudo leer la base de datos", e);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    /**
     * Valida el SQLite descifrado y reemplaza la base local. Después la app debe reiniciarse,
     * porque la instancia de Room queda cerrada.
     */
    private void replaceDatabase(byte[] plain) throws BackupException {
        if (plain.length < SQLITE_HEADER.length
                || !Arrays.equals(Arrays.copyOf(plain, SQLITE_HEADER.length), SQLITE_HEADER)) {
            throw new BackupException(Reason.INVALID_FILE, "El respaldo no contiene una base de datos válida");
        }
        File tmp = new File(context.getCacheDir(), "restore.db");
        try {
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(plain);
            }
            validate(tmp);

            database.close();
            File target = context.getDatabasePath(AppDatabase.NAME);
            for (String suffix : new String[]{"", "-wal", "-shm", "-journal"}) {
                //noinspection ResultOfMethodCallIgnored
                new File(target.getPath() + suffix).delete();
            }
            Files.copy(tmp.toPath(), target.toPath());
        } catch (IOException e) {
            throw new BackupException(Reason.UNKNOWN, "No se pudo restaurar la base de datos", e);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    private static void validate(File file) throws BackupException {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            int version = db.getVersion();
            if (version > AppDatabase.VERSION) {
                throw new BackupException(Reason.INCOMPATIBLE_VERSION,
                        "El respaldo es de una versión más nueva de la app");
            }
            try (Cursor c = db.rawQuery("PRAGMA integrity_check", null)) {
                if (!c.moveToFirst() || !"ok".equalsIgnoreCase(c.getString(0))) {
                    throw new BackupException(Reason.INVALID_FILE, "El respaldo está dañado");
                }
            }
            try (Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='periods'", null)) {
                if (!c.moveToFirst() || c.getInt(0) == 0) {
                    throw new BackupException(Reason.INVALID_FILE, "El respaldo no pertenece a esta app");
                }
            }
        } catch (BackupException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BackupException(Reason.INVALID_FILE, "El respaldo no se pudo abrir", e);
        } finally {
            if (db != null) db.close();
        }
    }
}
