package com.example.andrs.data.security;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado de respaldos con AES-256-GCM. La clave se deriva de la frase del usuario con
 * PBKDF2-HMAC-SHA256, de modo que el respaldo puede descifrarse en otro dispositivo
 * (una clave del Keystore quedaría atada a este teléfono).
 *
 * <pre>
 * Formato: "MCBK" | versión (1 byte) | iteraciones (int) | sal (16) | IV (12) | texto cifrado + tag (16)
 * La cabecera completa se autentica como AAD.
 * </pre>
 */
public final class BackupCrypto {
    private static final byte[] MAGIC = {'M', 'C', 'B', 'K'};
    private static final byte FORMAT_VERSION = 1;
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BITS = 256;
    static final int DEFAULT_ITERATIONS = 210_000;
    private static final int MAX_ITERATIONS = 5_000_000;
    private static final int HEADER_LEN = MAGIC.length + 1 + 4 + SALT_LEN + IV_LEN;

    public static final int MIN_PASSPHRASE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public BackupCrypto() {
        this(DEFAULT_ITERATIONS);
    }

    BackupCrypto(int iterations) {
        this.iterations = iterations;
    }

    public static class WrongPassphraseException extends GeneralSecurityException {
        public WrongPassphraseException(Throwable cause) {
            super("Frase incorrecta o archivo alterado", cause);
        }
    }

    public static class InvalidFormatException extends GeneralSecurityException {
        public InvalidFormatException(String msg) {
            super(msg);
        }
    }

    public byte[] encrypt(byte[] plain, char[] passphrase) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        random.nextBytes(salt);
        random.nextBytes(iv);

        ByteBuffer header = ByteBuffer.allocate(HEADER_LEN);
        header.put(MAGIC).put(FORMAT_VERSION).putInt(iterations).put(salt).put(iv);
        byte[] headerBytes = header.array();

        SecretKey key = deriveKey(passphrase, salt, iterations);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(headerBytes);
        byte[] cipherText = cipher.doFinal(plain);

        byte[] out = new byte[headerBytes.length + cipherText.length];
        System.arraycopy(headerBytes, 0, out, 0, headerBytes.length);
        System.arraycopy(cipherText, 0, out, headerBytes.length, cipherText.length);
        return out;
    }

    public byte[] decrypt(byte[] data, char[] passphrase) throws GeneralSecurityException {
        if (data.length < HEADER_LEN + TAG_BITS / 8) {
            throw new InvalidFormatException("Archivo demasiado corto");
        }
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte[] magic = new byte[MAGIC.length];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new InvalidFormatException("No es un respaldo de esta app");
        }
        byte version = buf.get();
        if (version != FORMAT_VERSION) {
            throw new InvalidFormatException("Versión de respaldo no soportada: " + version);
        }
        int iter = buf.getInt();
        if (iter < 10_000 || iter > MAX_ITERATIONS) {
            throw new InvalidFormatException("Parámetros de cifrado inválidos");
        }
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        buf.get(salt);
        buf.get(iv);

        SecretKey key = deriveKey(passphrase, salt, iter);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(data, 0, HEADER_LEN);
        try {
            return cipher.doFinal(data, HEADER_LEN, data.length - HEADER_LEN);
        } catch (AEADBadTagException e) {
            throw new WrongPassphraseException(e);
        }
    }

    private static SecretKey deriveKey(char[] passphrase, byte[] salt, int iterations)
            throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(passphrase, salt, iterations, KEY_BITS);
        try {
            byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            ((PBEKeySpec) spec).clearPassword();
        }
    }
}
