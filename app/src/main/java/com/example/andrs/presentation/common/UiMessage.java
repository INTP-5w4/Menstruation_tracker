package com.example.andrs.presentation.common;

import android.content.Context;

import androidx.annotation.StringRes;

/** Mensaje para la UI expresado como recurso de texto (los ViewModel no conocen el Context). */
public final class UiMessage {
    @StringRes public final int resId;
    public final Object[] args;

    public UiMessage(@StringRes int resId, Object... args) {
        this.resId = resId;
        this.args = args;
    }

    public String resolve(Context context) {
        return context.getString(resId, args);
    }
}
