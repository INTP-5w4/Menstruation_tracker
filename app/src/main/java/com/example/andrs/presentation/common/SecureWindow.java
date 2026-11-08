package com.example.andrs.presentation.common;

import android.app.Activity;
import android.view.WindowManager;

import com.example.andrs.App;

/** Aplica FLAG_SECURE (sin capturas ni vista previa en recientes) según los ajustes. */
public final class SecureWindow {
    private SecureWindow() {}

    public static void apply(Activity activity) {
        if (App.container(activity).settings.isScreenshotBlockEnabled()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
