package com.example.andrs.presentation.settings;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.NumberPicker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.andrs.App;
import com.example.andrs.BuildConfig;
import com.example.andrs.R;
import com.example.andrs.data.security.BackupCrypto;
import com.example.andrs.databinding.DialogDeleteAllBinding;
import com.example.andrs.databinding.DialogPassphraseBinding;
import com.example.andrs.databinding.FragmentSettingsBinding;
import com.example.andrs.databinding.RowSettingBinding;
import com.example.andrs.databinding.RowSwitchBinding;
import com.example.andrs.di.AppContainer;
import com.example.andrs.domain.model.BackupInfo;
import com.example.andrs.domain.model.CycleSettings;
import com.example.andrs.presentation.common.AppRestarter;
import com.example.andrs.presentation.common.Formats;
import com.example.andrs.presentation.common.UiMessage;
import com.example.andrs.presentation.common.ViewModelFactory;
import com.example.andrs.presentation.lock.BiometricHelper;
import com.example.andrs.presentation.onboarding.PrivacyNoticeActivity;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsViewModel vm;
    private AppContainer container;
    private SettingsViewModel.State state;
    /** Evita disparar listeners de switches al rellenarlos desde el estado. */
    private boolean binding_;

    private final ActivityResultLauncher<IntentSenderRequest> authLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                try {
                    AuthorizationResult r = container.driveAuth.resultFromIntent(result.getData());
                    if (r.getAccessToken() != null) onAuthorized();
                } catch (Exception e) {
                    toast(getString(R.string.drive_connect_failed, String.valueOf(e.getMessage())));
                }
            });

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                if (uri != null) exportTo(uri);
            });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) confirmImport(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        container = App.container(requireContext());
        vm = new ViewModelProvider(this, new ViewModelFactory(container)).get(SettingsViewModel.class);

        setupStaticTexts();
        setupListeners();

        vm.getState().observe(getViewLifecycleOwner(), this::render);
        vm.getBusy().observe(getViewLifecycleOwner(), b -> {
            boolean busy = Boolean.TRUE.equals(b);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.btnBackupNow.setEnabled(!busy);
            binding.btnRestoreDrive.setEnabled(!busy);
            binding.btnExport.setEnabled(!busy);
            binding.btnImport.setEnabled(!busy);
            binding.btnDrive.setEnabled(!busy);
        });
        vm.getMessages().observe(getViewLifecycleOwner(), e -> {
            UiMessage m = e.consume();
            if (m != null) toast(m.resolve(requireContext()));
        });
        vm.getRemoteFound().observe(getViewLifecycleOwner(), e -> {
            BackupInfo info = e.consume();
            if (info != null) offerRestore(info);
        });
        vm.getRestart().observe(getViewLifecycleOwner(), e -> {
            if (Boolean.TRUE.equals(e.consume())) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.restore_done)
                        .setCancelable(false)
                        .setPositiveButton(R.string.accept, (d, w) -> AppRestarter.restart(requireActivity()))
                        .show();
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && vm != null) vm.reload();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ---------------------------------------------------------------------------------------------

    private void setupStaticTexts() {
        title(binding.rowCycleLength, R.string.settings_cycle_length);
        title(binding.rowPeriodLength, R.string.settings_period_length);
        title(binding.rowLutealLength, R.string.settings_luteal_length);
        sw(binding.swPhaseAlerts, R.string.settings_phase_alerts, 0);
        sw(binding.swPeriodAlerts, R.string.settings_period_alerts, 0);
        sw(binding.swDiscreet, R.string.settings_discreet, R.string.settings_discreet_hint);
        sw(binding.swAppLock, R.string.settings_app_lock, R.string.settings_app_lock_hint);
        sw(binding.swBlockScreenshots, R.string.settings_block_screenshots, R.string.settings_block_screenshots_hint);
        title(binding.rowPrivacy, R.string.settings_view_privacy);
        binding.rowPrivacy.summary.setVisibility(View.GONE);
        title(binding.rowPassphrase, R.string.settings_passphrase);
        sw(binding.swAutoBackup, R.string.settings_auto_backup, 0);
        binding.version.setText(getString(R.string.settings_version, BuildConfig.VERSION_NAME));
    }

    private void setupListeners() {
        binding.rowCycleLength.getRoot().setOnClickListener(v -> pickNumber(R.string.settings_cycle_length,
                21, 45, state.cycle.getCycleLength(), n -> vm.saveCycleSettings(
                        new CycleSettings(n, state.cycle.getPeriodLength(), state.cycle.getLutealLength()))));
        binding.rowPeriodLength.getRoot().setOnClickListener(v -> pickNumber(R.string.settings_period_length,
                2, 10, state.cycle.getPeriodLength(), n -> vm.saveCycleSettings(
                        new CycleSettings(state.cycle.getCycleLength(), n, state.cycle.getLutealLength()))));
        binding.rowLutealLength.getRoot().setOnClickListener(v -> pickNumber(R.string.settings_luteal_length,
                10, 16, state.cycle.getLutealLength(), n -> vm.saveCycleSettings(
                        new CycleSettings(state.cycle.getCycleLength(), state.cycle.getPeriodLength(), n))));

        onToggle(binding.swPhaseAlerts, vm::setPhaseAlerts);
        onToggle(binding.swPeriodAlerts, vm::setPeriodAlerts);
        onToggle(binding.swDiscreet, vm::setDiscreet);
        onToggle(binding.swAppLock, enabled -> {
            if (enabled && !BiometricHelper.isAvailable(requireContext())) {
                binding.swAppLock.toggle.setChecked(false);
                toast(getString(R.string.lock_unavailable));
                return;
            }
            if (enabled) App.lockManager(requireContext()).unlock();
            vm.setAppLock(enabled);
        });
        onToggle(binding.swBlockScreenshots, enabled -> {
            vm.setBlockScreenshots(enabled);
            binding.getRoot().postDelayed(() -> {
                if (getActivity() != null) com.example.andrs.presentation.common.SecureWindow.apply(getActivity());
            }, 300);
        });
        binding.rowPrivacy.getRoot().setOnClickListener(v ->
                startActivity(PrivacyNoticeActivity.readOnlyIntent(requireContext())));

        binding.btnDrive.setOnClickListener(v -> {
            if (state != null && state.driveLinked) confirmDisconnect();
            else connectDrive();
        });
        binding.rowPassphrase.getRoot().setOnClickListener(v -> askNewPassphrase(null));
        onToggle(binding.swAutoBackup, enabled -> {
            if (enabled && (state == null || !state.hasPassphrase)) {
                binding.swAutoBackup.toggle.setChecked(false);
                askNewPassphrase(() -> vm.setAutoBackup(true));
                return;
            }
            vm.setAutoBackup(enabled);
        });
        binding.btnBackupNow.setOnClickListener(v -> {
            if (!requireDrive()) return;
            if (!state.hasPassphrase) askNewPassphrase(vm::backupNow);
            else vm.backupNow();
        });
        binding.btnRestoreDrive.setOnClickListener(v -> {
            if (requireDrive()) vm.checkRemoteBackup();
        });
        binding.btnExport.setOnClickListener(v -> {
            App.lockManager(requireContext()).allowExternalFlow();
            String name = "mi_ciclo_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".mcbk";
            exportLauncher.launch(name);
        });
        binding.btnImport.setOnClickListener(v -> {
            App.lockManager(requireContext()).allowExternalFlow();
            importLauncher.launch(new String[]{"*/*"});
        });
        binding.btnDeleteAll.setOnClickListener(v -> confirmDeleteAll());
    }

    private void render(SettingsViewModel.State s) {
        state = s;
        binding_ = true;
        binding.rowCycleLength.summary.setText(getString(R.string.days_value, s.cycle.getCycleLength()));
        binding.rowPeriodLength.summary.setText(getString(R.string.days_value, s.cycle.getPeriodLength()));
        binding.rowLutealLength.summary.setText(getString(R.string.days_value, s.cycle.getLutealLength()));
        binding.swPhaseAlerts.toggle.setChecked(s.phaseAlerts);
        binding.swPeriodAlerts.toggle.setChecked(s.periodAlerts);
        binding.swDiscreet.toggle.setChecked(s.discreet);
        binding.swAppLock.toggle.setChecked(s.appLock);
        binding.swBlockScreenshots.toggle.setChecked(s.blockScreenshots);
        binding.swAutoBackup.toggle.setChecked(s.autoBackup);
        binding.swAutoBackup.toggle.setEnabled(s.driveLinked);

        binding.driveStatus.setText(s.driveLinked ? R.string.settings_drive_connected : R.string.settings_drive_disconnected);
        binding.btnDrive.setText(s.driveLinked ? R.string.settings_drive_disconnect : R.string.settings_drive_connect);
        binding.rowPassphrase.summary.setText(s.hasPassphrase ? R.string.settings_passphrase_set : R.string.settings_passphrase_missing);
        if (s.lastBackup > 0) {
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(s.lastBackup), ZoneId.systemDefault());
            binding.lastBackup.setText(getString(R.string.settings_last_backup, Formats.dateTime(dt)));
        } else {
            binding.lastBackup.setText(R.string.settings_last_backup_never);
        }
        binding.lastError.setVisibility(s.lastError != null ? View.VISIBLE : View.GONE);
        if (s.lastError != null) binding.lastError.setText(getString(R.string.settings_last_error, s.lastError));
        binding_ = false;
    }

    // --- Drive -----------------------------------------------------------------------------------

    private void connectDrive() {
        App.lockManager(requireContext()).allowExternalFlow();
        container.driveAuth.authorize()
                .addOnSuccessListener(r -> {
                    if (r.hasResolution() && r.getPendingIntent() != null) {
                        authLauncher.launch(new IntentSenderRequest.Builder(
                                r.getPendingIntent().getIntentSender()).build());
                    } else {
                        onAuthorized();
                    }
                })
                .addOnFailureListener(e -> toast(getString(R.string.drive_connect_failed, String.valueOf(e.getMessage()))));
    }

    private void onAuthorized() {
        vm.onDriveAuthorized();
    }

    private void confirmDisconnect() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.drive_disconnect_confirm)
                .setPositiveButton(R.string.settings_drive_disconnect, (d, w) -> vm.disconnectDrive(false))
                .setNeutralButton(R.string.drive_disconnect_and_delete, (d, w) -> vm.disconnectDrive(true))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean requireDrive() {
        if (state == null || !state.driveLinked) {
            toast(getString(R.string.drive_need_connection));
            return false;
        }
        return true;
    }

    private void offerRestore(BackupInfo info) {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(info.modifiedTimeMillis), ZoneId.systemDefault());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_found_title)
                .setMessage(getString(R.string.backup_found_text, Formats.dateTime(dt)))
                .setPositiveButton(R.string.restore, (d, w) -> askExistingPassphrase(vm::restoreFromDrive))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // --- Archivo local ---------------------------------------------------------------------------

    private void exportTo(Uri uri) {
        char[] saved = vm.savedPassphrase();
        if (saved != null) {
            vm.exportTo(uri, saved);
        } else {
            askNewPassphraseThen(pass -> vm.exportTo(uri, pass));
        }
    }

    private void confirmImport(Uri uri) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.restore_confirm_title)
                .setMessage(R.string.restore_confirm_text)
                .setPositiveButton(R.string.restore, (d, w) -> askExistingPassphrase(pass -> vm.importFrom(uri, pass)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // --- Diálogos --------------------------------------------------------------------------------

    /** Configura una frase nueva (con confirmación) y la guarda. */
    private void askNewPassphrase(@Nullable Runnable after) {
        askNewPassphraseThen(pass -> {
            vm.setPassphrase(pass.clone());
            java.util.Arrays.fill(pass, '\0');
            if (after != null) after.run();
        });
    }

    private void askNewPassphraseThen(Consumer<char[]> onValid) {
        DialogPassphraseBinding d = DialogPassphraseBinding.inflate(getLayoutInflater());
        d.explain.setText(R.string.passphrase_explain);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.passphrase_title)
                .setView(d.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            char[] p = chars(d.pass.getText());
            char[] c = chars(d.confirm.getText());
            d.passLayout.setError(null);
            d.confirmLayout.setError(null);
            if (p.length < BackupCrypto.MIN_PASSPHRASE_LENGTH) {
                d.passLayout.setError(getString(R.string.passphrase_too_short));
                return;
            }
            if (!java.util.Arrays.equals(p, c)) {
                d.confirmLayout.setError(getString(R.string.passphrase_mismatch));
                return;
            }
            java.util.Arrays.fill(c, '\0');
            dialog.dismiss();
            onValid.accept(p);
        });
    }

    private void askExistingPassphrase(Consumer<char[]> onEntered) {
        DialogPassphraseBinding d = DialogPassphraseBinding.inflate(getLayoutInflater());
        d.explain.setText(R.string.passphrase_enter_explain);
        d.confirmLayout.setVisibility(View.GONE);
        d.passLayout.setHint(getString(R.string.settings_passphrase));
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.passphrase_enter_title)
                .setView(d.getRoot())
                .setPositiveButton(R.string.continue_, null)
                .setNegativeButton(R.string.cancel, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            char[] p = chars(d.pass.getText());
            if (p.length == 0) return;
            dialog.dismiss();
            onEntered.accept(p);
        });
    }

    private void confirmDeleteAll() {
        DialogDeleteAllBinding d = DialogDeleteAllBinding.inflate(getLayoutInflater());
        d.deleteDrive.setVisibility(state != null && state.driveLinked ? View.VISIBLE : View.GONE);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_all_title)
                .setView(d.getRoot())
                .setPositiveButton(R.string.delete, null)
                .setNegativeButton(R.string.cancel, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Editable t = d.confirm.getText();
            if (t == null || !getString(R.string.delete_all_keyword).equalsIgnoreCase(t.toString().trim())) {
                d.confirm.setError(getString(R.string.delete_all_keyword));
                return;
            }
            dialog.dismiss();
            vm.deleteAll(d.deleteDrive.isChecked());
        });
    }

    private void pickNumber(@StringRes int title, int min, int max, int value, Consumer<Integer> onPicked) {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(Math.max(min, Math.min(max, value)));
        picker.setWrapSelectorWheel(false);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(picker)
                .setPositiveButton(R.string.save, (d, w) -> onPicked.accept(picker.getValue()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // --- Utilidades ------------------------------------------------------------------------------

    private void title(RowSettingBinding row, @StringRes int text) {
        row.title.setText(text);
    }

    private void sw(RowSwitchBinding row, @StringRes int text, @StringRes int summary) {
        row.toggle.setText(text);
        if (summary != 0) {
            row.summary.setText(summary);
            row.summary.setVisibility(View.VISIBLE);
        }
    }

    private void onToggle(RowSwitchBinding row, Consumer<Boolean> listener) {
        row.toggle.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> {
            if (!binding_) listener.accept(checked);
        });
    }

    private static char[] chars(@Nullable Editable e) {
        if (e == null) return new char[0];
        char[] out = new char[e.length()];
        e.getChars(0, e.length(), out, 0);
        return out;
    }

    private void toast(String text) {
        if (binding != null) Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG).show();
    }
}
