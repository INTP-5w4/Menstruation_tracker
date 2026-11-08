package com.example.andrs.domain.repository;

import androidx.annotation.Nullable;

import com.example.andrs.domain.model.BackupInfo;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Respaldo completo de la base de datos, cifrado con AES-256-GCM usando una clave derivada de la
 * frase de respaldo del usuario (así puede restaurarse en otro dispositivo).
 */
public interface BackupRepository {
    /** Sube un respaldo cifrado a la carpeta oculta de la app en Google Drive. */
    void backupToDrive() throws BackupException;

    @Nullable BackupInfo findDriveBackup() throws BackupException;

    /** Descarga, descifra y reemplaza la base de datos local. Requiere reiniciar la app. */
    void restoreFromDrive(char[] passphrase) throws BackupException;

    void deleteDriveBackup() throws BackupException;

    void exportEncrypted(OutputStream out, char[] passphrase) throws BackupException;

    void importEncrypted(InputStream in, char[] passphrase) throws BackupException;
}
