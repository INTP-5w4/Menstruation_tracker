package com.example.andrs.presentation.common;

import android.app.Activity;
import android.content.Intent;

/** Reinicia el proceso tras restaurar o borrar la base de datos. */
public final class AppRestarter {
    private AppRestarter() {}

    public static void restart(Activity activity) {
        Intent launch = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (launch != null && launch.getComponent() != null) {
            activity.startActivity(Intent.makeRestartActivityTask(launch.getComponent()));
        }
        activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }
}
