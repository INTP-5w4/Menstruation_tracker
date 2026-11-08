package com.example.andrs.worker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.andrs.R;
import com.example.andrs.presentation.main.MainActivity;

/**
 * Notificaciones locales. Todas usan visibilidad privada con una versión pública genérica, para
 * que la pantalla de bloqueo nunca muestre detalles de salud.
 */
public final class NotificationHelper {
    public static final String CHANNEL_MEDS = "medications";
    public static final String CHANNEL_CYCLE = "cycle";
    public static final String CHANNEL_BACKUP = "backup";

    private static final int ID_CYCLE_BASE = 20_000;
    public static final int ID_BACKUP = 30_000;
    private static final int ID_MED_BASE = 100_000;

    private NotificationHelper() {}

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        NotificationChannel meds = new NotificationChannel(CHANNEL_MEDS,
                context.getString(R.string.channel_meds), NotificationManager.IMPORTANCE_HIGH);
        meds.setDescription(context.getString(R.string.channel_meds_desc));
        meds.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        NotificationChannel cycle = new NotificationChannel(CHANNEL_CYCLE,
                context.getString(R.string.channel_cycle), NotificationManager.IMPORTANCE_DEFAULT);
        cycle.setDescription(context.getString(R.string.channel_cycle_desc));
        cycle.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        NotificationChannel backup = new NotificationChannel(CHANNEL_BACKUP,
                context.getString(R.string.channel_backup), NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(meds);
        nm.createNotificationChannel(cycle);
        nm.createNotificationChannel(backup);
    }

    public static int medicationNotificationId(long medicationId) {
        return ID_MED_BASE + (int) (medicationId % 100_000);
    }

    public static void showMedicationReminder(Context context, long medicationId, String name,
                                              boolean discreet) {
        int id = medicationNotificationId(medicationId);
        String title = context.getString(R.string.notif_med_title);
        String text = discreet ? context.getString(R.string.notif_med_text_discreet)
                : context.getString(R.string.notif_med_text, name);

        Intent taken = new Intent(context, MedicationActionReceiver.class)
                .setAction(MedicationActionReceiver.ACTION_TAKEN)
                .putExtra(MedicationActionReceiver.EXTRA_MEDICATION_ID, medicationId);
        PendingIntent takenPi = PendingIntent.getBroadcast(context, id, taken,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = base(context, CHANNEL_MEDS, title, text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .addAction(0, context.getString(R.string.notif_med_action_taken), takenPi);
        notify(context, id, b);
    }

    public static void showCycleAlert(Context context, int slot, String title, String text, boolean discreet) {
        NotificationCompat.Builder b = base(context, CHANNEL_CYCLE,
                discreet ? context.getString(R.string.notif_generic_title) : title,
                discreet ? context.getString(R.string.notif_generic_text) : text);
        notify(context, ID_CYCLE_BASE + slot, b);
    }

    public static void showBackupProblem(Context context, String text) {
        NotificationCompat.Builder b = base(context, CHANNEL_BACKUP,
                context.getString(R.string.notif_backup_title), text);
        notify(context, ID_BACKUP, b);
    }

    public static void cancel(Context context, int id) {
        NotificationManagerCompat.from(context).cancel(id);
    }

    private static NotificationCompat.Builder base(Context context, String channel, String title, String text) {
        Intent open = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder publicVersion = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notif_public_text));

        return new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.brand_primary))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(contentPi)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion.build());
    }

    private static void notify(Context context, int id, NotificationCompat.Builder builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
