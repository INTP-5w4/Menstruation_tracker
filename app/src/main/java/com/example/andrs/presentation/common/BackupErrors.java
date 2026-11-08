package com.example.andrs.presentation.common;

import com.example.andrs.R;
import com.example.andrs.domain.repository.BackupException;

public final class BackupErrors {
    private BackupErrors() {}

    public static UiMessage toMessage(BackupException e) {
        switch (e.getReason()) {
            case NEEDS_AUTHORIZATION: return new UiMessage(R.string.err_needs_auth);
            case NO_PASSPHRASE: return new UiMessage(R.string.err_no_passphrase);
            case WRONG_PASSPHRASE: return new UiMessage(R.string.err_wrong_passphrase);
            case NOT_FOUND: return new UiMessage(R.string.err_not_found);
            case NETWORK: return new UiMessage(R.string.err_network);
            case INVALID_FILE: return new UiMessage(R.string.err_invalid_file);
            case INCOMPATIBLE_VERSION: return new UiMessage(R.string.err_incompatible);
            default: return new UiMessage(R.string.err_unknown, String.valueOf(e.getMessage()));
        }
    }
}
