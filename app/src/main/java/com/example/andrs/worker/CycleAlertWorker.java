package com.example.andrs.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.CyclePhase;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.repository.SettingsRepository;
import com.example.andrs.presentation.common.Labels;

import java.time.LocalDate;

/**
 * Revisión diaria del ciclo: avisa cambios de fase, inicio de ventana fértil, regla próxima y
 * retrasos. Cada alerta se envía una sola vez gracias a una clave guardada en preferencias.
 */
public class CycleAlertWorker extends Worker {
    private static final int SLOT_PHASE = 1;
    private static final int SLOT_FERTILE = 2;
    private static final int SLOT_PERIOD = 3;

    public CycleAlertWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppContainer c = App.container(getApplicationContext());
        SettingsRepository s = c.settings;
        if (!s.isPrivacyNoticeAccepted()) return Result.success();
        boolean phaseAlerts = s.isPhaseAlertsEnabled();
        boolean periodAlerts = s.isPeriodAlertsEnabled();
        if (!phaseAlerts && !periodAlerts) return Result.success();

        LocalDate today = LocalDate.now();
        CycleStatus st = c.getCycleStatus.execute(c.cycles.getPeriodsSync(), s.getCycleSettings(), today);
        if (!st.hasData) return Result.success();

        Context ctx = getApplicationContext();
        boolean discreet = s.isDiscreetNotifications();

        if (phaseAlerts) {
            // La fase menstrual la registra el propio usuario: no se notifica.
            String phaseKey = st.phase.name() + "|" + st.lastPeriodStart;
            if (st.phase != CyclePhase.MENSTRUAL && st.phase != CyclePhase.UNKNOWN
                    && !phaseKey.equals(s.getLastAlertKey("phase"))) {
                String name = ctx.getString(Labels.phase(st.phase));
                NotificationHelper.showCycleAlert(ctx, SLOT_PHASE,
                        ctx.getString(R.string.notif_phase_title, name),
                        ctx.getString(Labels.phaseDescription(st.phase)), discreet);
                s.setLastAlertKey("phase", phaseKey);
            }
            if (today.equals(st.fertileStart)) {
                String key = "fertile|" + today;
                if (!key.equals(s.getLastAlertKey("fertile"))) {
                    NotificationHelper.showCycleAlert(ctx, SLOT_FERTILE,
                            ctx.getString(R.string.notif_fertile_title),
                            ctx.getString(R.string.notif_fertile_text), discreet);
                    s.setLastAlertKey("fertile", key);
                }
            }
        }

        if (periodAlerts && !st.periodOngoing) {
            String title = null;
            String text = null;
            if (st.daysUntilNextPeriod == 2 || st.daysUntilNextPeriod == 1) {
                title = ctx.getString(R.string.notif_period_soon_title);
                text = ctx.getResources().getQuantityString(R.plurals.notif_period_soon_text,
                        st.daysUntilNextPeriod, st.daysUntilNextPeriod);
            } else if (st.daysUntilNextPeriod == 0) {
                title = ctx.getString(R.string.notif_period_today_title);
                text = ctx.getString(R.string.notif_period_today_text);
            } else if (st.isLate && (st.daysLate == 3 || st.daysLate == 7 || st.daysLate == 14)) {
                title = ctx.getString(R.string.notif_period_late_title);
                text = ctx.getResources().getQuantityString(R.plurals.notif_period_late_text,
                        st.daysLate, st.daysLate);
            }
            String key = "period|" + today;
            if (title != null && !key.equals(s.getLastAlertKey("period"))) {
                NotificationHelper.showCycleAlert(ctx, SLOT_PERIOD, title, text, discreet);
                s.setLastAlertKey("period", key);
            }
        }
        return Result.success();
    }
}
