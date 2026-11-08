package com.example.andrs.domain.model;

/** Metadatos de un respaldo remoto. */
public final class BackupInfo {
    public final String remoteId;
    public final long modifiedTimeMillis;
    public final long sizeBytes;

    public BackupInfo(String remoteId, long modifiedTimeMillis, long sizeBytes) {
        this.remoteId = remoteId;
        this.modifiedTimeMillis = modifiedTimeMillis;
        this.sizeBytes = sizeBytes;
    }
}
