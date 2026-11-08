package com.example.andrs.data.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BackupCryptoTest {
    // Pocas iteraciones para que la prueba sea rápida; el formato las guarda en la cabecera.
    private final BackupCrypto crypto = new BackupCrypto(20_000);
    private final byte[] data = "SQLite format 3\u0000 datos de prueba".getBytes(StandardCharsets.UTF_8);

    @Test
    public void roundTrip() throws Exception {
        byte[] enc = crypto.encrypt(data, "frase segura".toCharArray());
        assertFalse(Arrays.equals(Arrays.copyOfRange(enc, enc.length - data.length, enc.length), data));
        assertArrayEquals(data, crypto.decrypt(enc, "frase segura".toCharArray()));
    }

    @Test
    public void decryptsWithDefaultInstance() throws Exception {
        // Otro dispositivo (otra instancia) puede descifrar con la misma frase.
        byte[] enc = crypto.encrypt(data, "frase segura".toCharArray());
        assertArrayEquals(data, new BackupCrypto().decrypt(enc, "frase segura".toCharArray()));
    }

    @Test
    public void wrongPassphrase_fails() throws Exception {
        byte[] enc = crypto.encrypt(data, "frase segura".toCharArray());
        assertThrows(BackupCrypto.WrongPassphraseException.class,
                () -> crypto.decrypt(enc, "otra frase".toCharArray()));
    }

    @Test
    public void tamperedHeader_fails() throws Exception {
        byte[] enc = crypto.encrypt(data, "frase segura".toCharArray());
        enc[20] ^= 1; // dentro de la sal: se autentica como AAD
        assertThrows(BackupCrypto.WrongPassphraseException.class,
                () -> crypto.decrypt(enc, "frase segura".toCharArray()));
    }

    @Test
    public void notABackup_fails() {
        assertThrows(BackupCrypto.InvalidFormatException.class,
                () -> crypto.decrypt(new byte[64], "frase segura".toCharArray()));
    }
}
