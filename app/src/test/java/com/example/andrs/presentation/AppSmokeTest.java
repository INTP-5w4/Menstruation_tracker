package com.example.andrs.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.TestApp;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.Medication;
import com.example.andrs.domain.model.MedicationCategory;
import com.example.andrs.presentation.main.MainActivity;
import com.example.andrs.presentation.onboarding.PrivacyNoticeActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Arranca la app real y recorre sus pantallas para detectar errores en tiempo de ejecución. */
@RunWith(AndroidJUnit4.class)
@Config(application = TestApp.class, sdk = 37)
public class AppSmokeTest {

    private static AppContainer container() {
        return App.container(ApplicationProvider.getApplicationContext());
    }

    /** Ejecuta consultas de Room fuera del hilo principal (Room lo exige). */
    private static <T> T io(java.util.concurrent.Callable<T> call) {
        try {
            return java.util.concurrent.Executors.newSingleThreadExecutor().submit(call).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitFor(BooleanSupplier condition) {
        long end = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle();
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
        }
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue("La condición no se cumplió a tiempo", condition.getAsBoolean());
    }

    @Test
    public void firstLaunch_showsMandatoryPrivacyNotice() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            Intent next = shadowOf((android.app.Application) ApplicationProvider.getApplicationContext())
                    .getNextStartedActivity();
            assertNotNull(next);
            assertEquals(PrivacyNoticeActivity.class.getName(), next.getComponent().getClassName());
        }
    }

    @Test
    public void privacyNotice_acceptRequiresScrollAndCheckbox() {
        try (ActivityScenario<PrivacyNoticeActivity> s = ActivityScenario.launch(PrivacyNoticeActivity.class)) {
            s.onActivity(a -> {
                View accept = a.findViewById(R.id.btn_accept);
                assertFalse(accept.isEnabled());
                NestedScrollView scroll = a.findViewById(R.id.scroll);
                scroll.fullScroll(View.FOCUS_DOWN);
                scroll.scrollTo(0, 100_000);
            });
            shadowOf(Looper.getMainLooper()).idle();
            s.onActivity(a -> {
                android.widget.CheckBox cb = a.findViewById(R.id.checkbox);
                assertTrue(cb.isEnabled());
                cb.setChecked(true);
                View accept = a.findViewById(R.id.btn_accept);
                assertTrue(accept.isEnabled());
                accept.performClick();
            });
            waitFor(() -> container().settings.isPrivacyNoticeAccepted());
        }
    }

    @Test
    public void mainFlow_allTabsRender() {
        container().settings.acceptPrivacyNotice();
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            shadowOf(Looper.getMainLooper()).idle();

            // Inicio sin datos -> registrar inicio de regla hoy.
            s.onActivity(a -> a.findViewById(R.id.btn_period_today).performClick());
            waitFor(() -> !io(() -> container().cycles.getPeriodsSync()).isEmpty());
            AtomicReference<String> phase = new AtomicReference<>();
            waitFor(() -> {
                s.onActivity(a -> phase.set(((TextView) a.findViewById(R.id.phase_name)).getText().toString()));
                return phase.get().contains("Menstrual");
            });

            // Un medicamento con recordatorio para poblar la pestaña.
            container().executors.diskIO().execute(() -> {
                long id = container().medications.save(new Medication(0, "Píldora", "1 tableta",
                        MedicationCategory.HORMONAL_CONTRACEPTIVE, true, true, 21, 0, true, null));
                container().reminders.scheduleMedication(container().medications.getSync(id));
                container().medications.logIntake(id, java.time.LocalDateTime.now(), null);
            });
            waitFor(() -> io(() -> container().medications.getAllSync()).size() == 1);

            int[] tabs = {R.id.nav_calendar, R.id.nav_log, R.id.nav_meds, R.id.nav_settings, R.id.nav_home};
            for (int tab : tabs) {
                s.onActivity(a -> ((BottomNavigationView) a.findViewById(R.id.bottom_nav)).setSelectedItemId(tab));
                shadowOf(Looper.getMainLooper()).idle();
            }

            // Registro diario: guardar síntomas de hoy.
            s.onActivity(a -> a.openLog(LocalDate.now()));
            shadowOf(Looper.getMainLooper()).idle();
            s.onActivity(a -> {
                com.google.android.material.chip.ChipGroup group = a.findViewById(R.id.symptom_group);
                ((com.google.android.material.chip.Chip) group.getChildAt(0)).setChecked(true);
                a.findViewById(R.id.fab_save).performClick();
            });
            waitFor(() -> {
                com.example.andrs.domain.model.DailyLog l = io(() -> container().dailyLogs.getSync(LocalDate.now()));
                return l != null && !l.getSymptoms().isEmpty();
            });

            // Diálogos principales se abren sin errores.
            s.onActivity(a -> ((BottomNavigationView) a.findViewById(R.id.bottom_nav)).setSelectedItemId(R.id.nav_meds));
            shadowOf(Looper.getMainLooper()).idle();
            s.onActivity(a -> a.findViewById(R.id.fab_add).performClick());
            shadowOf(Looper.getMainLooper()).idle();
            assertNotNull(ShadowDialog.getLatestDialog());
            ShadowDialog.getLatestDialog().dismiss();

            s.onActivity(a -> ((BottomNavigationView) a.findViewById(R.id.bottom_nav)).setSelectedItemId(R.id.nav_settings));
            shadowOf(Looper.getMainLooper()).idle();
            s.onActivity(a -> a.findViewById(R.id.row_passphrase).performClick());
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue(ShadowDialog.getLatestDialog().isShowing());
        }
    }
}
