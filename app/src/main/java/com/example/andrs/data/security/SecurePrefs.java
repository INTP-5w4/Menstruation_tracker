package com.example.andrs.data.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * SharedPreferences cifradas (claves AES-256-SIV, valores AES-256-GCM) con la clave maestra
 * guardada en el Android Keystore. Nunca salen del dispositivo.
 */
@SuppressWarnings("deprecation") // security-crypto 1.1.0 marca la API como obsoleta, pero sigue siendo funcional.
public final class SecurePrefs {
    private static final String TAG = "SecurePrefs";
    private static final String FILE_NAME = "secure_prefs";

    private SecurePrefs() {}

    public static SharedPreferences create(Context context) {
        Context app = context.getApplicationContext();
        try {
            return open(app);
        } catch (Exception first) {
            // Puede ocurrir si el Keystore perdió la clave (p. ej. tras restaurar el sistema).
            // Se descartan las preferencias corruptas y se recrean.
            Log.w(TAG, "Preferencias cifradas ilegibles; se recrean", first);
            app.deleteSharedPreferences(FILE_NAME);
            try {
                return open(app);
            } catch (Exception second) {
                throw new IllegalStateException("No se pudo inicializar el almacenamiento cifrado", second);
            }
        }
    }

    private static SharedPreferences open(Context app) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                app,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }
}
