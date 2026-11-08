package com.example.andrs.presentation.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.databinding.ActivityMainBinding;
import com.example.andrs.di.AppContainer;
import com.example.andrs.presentation.calendar.CalendarFragment;
import com.example.andrs.presentation.common.SecureWindow;
import com.example.andrs.presentation.common.ViewModelFactory;
import com.example.andrs.presentation.home.HomeFragment;
import com.example.andrs.presentation.lock.AppLockManager;
import com.example.andrs.presentation.lock.BiometricHelper;
import com.example.andrs.presentation.log.LogFragment;
import com.example.andrs.presentation.meds.MedicationsFragment;
import com.example.andrs.presentation.onboarding.PrivacyNoticeActivity;
import com.example.andrs.presentation.settings.SettingsFragment;

import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    private static final String STATE_TAB = "tab";
    private static final String NOTIF_ASKED_KEY = "notification_permission";

    private ActivityMainBinding binding;
    private AppContainer container;
    private AppLockManager lock;
    private MainViewModel mainViewModel;
    private boolean authenticating;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        container = App.container(this);
        lock = App.lockManager(this);

        if (!container.settings.isPrivacyNoticeAccepted()) {
            startActivity(new Intent(this, PrivacyNoticeActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            binding.fragmentContainer.setPadding(bars.left, bars.top, bars.right, 0);
            binding.bottomNav.setPadding(bars.left, 0, bars.right, bars.bottom);
            binding.lockOverlay.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        mainViewModel = new ViewModelProvider(this, new ViewModelFactory(container)).get(MainViewModel.class);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        int tab = savedInstanceState != null ? savedInstanceState.getInt(STATE_TAB, R.id.nav_home) : R.id.nav_home;
        binding.bottomNav.setSelectedItemId(tab);
        binding.btnUnlock.setOnClickListener(v -> promptUnlock());

        maybeAskNotificationPermission();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) outState.putInt(STATE_TAB, binding.bottomNav.getSelectedItemId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) return;
        SecureWindow.apply(this);
        if (lock.shouldLock()) {
            binding.lockOverlay.setVisibility(View.VISIBLE);
            if (!authenticating) promptUnlock();
        } else {
            binding.lockOverlay.setVisibility(View.GONE);
        }
    }

    /** Abre el registro diario en la fecha indicada (desde el inicio o el calendario). */
    public void openLog(LocalDate date) {
        mainViewModel.setLogDate(date);
        binding.bottomNav.setSelectedItemId(R.id.nav_log);
    }

    public void openTab(@IdRes int id) {
        binding.bottomNav.setSelectedItemId(id);
    }

    private void promptUnlock() {
        if (!BiometricHelper.isAvailable(this)) {
            // Sin bloqueo de pantalla configurado no se puede autenticar: se desactiva para no
            // dejar a la persona fuera de sus propios datos.
            container.settings.setAppLockEnabled(false);
            lock.unlock();
            binding.lockOverlay.setVisibility(View.GONE);
            return;
        }
        authenticating = true;
        BiometricHelper.authenticate(this, new BiometricHelper.Callback() {
            @Override
            public void onSuccess() {
                authenticating = false;
                lock.unlock();
                binding.lockOverlay.setVisibility(View.GONE);
            }

            @Override
            public void onFailure() {
                authenticating = false;
            }
        });
    }

    private void showTab(@IdRes int id) {
        String tag = "tab_" + id;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);
        for (Fragment f : fm.getFragments()) {
            if (!tag.equals(f.getTag())) tx.hide(f);
        }
        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) {
            tx.add(R.id.fragment_container, createTab(id), tag);
        } else {
            tx.show(target);
        }
        tx.commit();
    }

    private Fragment createTab(@IdRes int id) {
        if (id == R.id.nav_calendar) return new CalendarFragment();
        if (id == R.id.nav_log) return new LogFragment();
        if (id == R.id.nav_meds) return new MedicationsFragment();
        if (id == R.id.nav_settings) return new SettingsFragment();
        return new HomeFragment();
    }

    private void maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        container.executors.diskIO().execute(() -> {
            if (container.settings.getLastAlertKey(NOTIF_ASKED_KEY) != null) return;
            container.settings.setLastAlertKey(NOTIF_ASKED_KEY, "asked");
            container.executors.mainThread().execute(() ->
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS));
        });
    }
}
