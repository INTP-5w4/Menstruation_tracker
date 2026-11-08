package com.example.andrs.domain.repository;

/** Error de respaldo/restauración con un motivo que la UI puede traducir. */
public class BackupException extends Exception {
    public enum Reason {
        NEEDS_AUTHORIZATION,
        NO_PASSPHRASE,
        WRONG_PASSPHRASE,
        NOT_FOUND,
        NETWORK,
        INVALID_FILE,
        INCOMPATIBLE_VERSION,
        UNKNOWN
    }

    private final Reason reason;

    public BackupException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public BackupException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
