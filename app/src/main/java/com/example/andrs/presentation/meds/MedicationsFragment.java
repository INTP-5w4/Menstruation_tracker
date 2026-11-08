package com.example.andrs.presentation.meds;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.databinding.DialogMedicationBinding;
import com.example.andrs.databinding.FragmentMedsBinding;
import com.example.andrs.domain.model.Medication;
import com.example.andrs.domain.model.MedicationCategory;
import com.example.andrs.presentation.common.DatePickers;
import com.example.andrs.presentation.common.Formats;
import com.example.andrs.presentation.common.Labels;
import com.example.andrs.presentation.common.UiMessage;
import com.example.andrs.presentation.common.ViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MedicationsFragment extends Fragment {
    private FragmentMedsBinding binding;
    private MedicationsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMedsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this, new ViewModelFactory(App.container(requireContext())))
                .get(MedicationsViewModel.class);

        MedicationAdapter meds = new MedicationAdapter(this::showEditor, vm::takeNow);
        binding.medsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.medsList.setAdapter(meds);

        IntakeAdapter intakes = new IntakeAdapter(i -> new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.intake_delete_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> vm.deleteIntake(i.getId()))
                .setNegativeButton(R.string.cancel, null)
                .show());
        binding.intakesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.intakesList.setAdapter(intakes);

        vm.getMedications().observe(getViewLifecycleOwner(), list -> {
            meds.submit(list);
            binding.medsEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.getIntakes().observe(getViewLifecycleOwner(), list -> {
            intakes.submit(list);
            binding.intakesEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.getMessages().observe(getViewLifecycleOwner(), e -> {
            UiMessage m = e.consume();
            if (m != null) Snackbar.make(binding.getRoot(), m.resolve(requireContext()), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.fabAdd).show();
        });

        binding.fabAdd.setOnClickListener(v -> showEditor(null));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showEditor(@Nullable Medication existing) {
        DialogMedicationBinding d = DialogMedicationBinding.inflate(getLayoutInflater());
        MedicationCategory[] categories = MedicationCategory.values();
        List<String> names = new ArrayList<>();
        for (MedicationCategory c : categories) names.add(getString(Labels.category(c)));
        d.category.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, names));

        final MedicationCategory[] category = {existing != null ? existing.getCategory() : MedicationCategory.OTHER};
        final int[] time = {existing != null ? existing.getReminderHour() : 9,
                existing != null ? existing.getReminderMinute() : 0};

        d.category.setText(getString(Labels.category(category[0])), false);
        d.category.setOnItemClickListener((parent, v, pos, id) -> {
            category[0] = categories[pos];
            d.switchAlters.setChecked(categories[pos].altersCycleByDefault());
        });

        if (existing != null) {
            d.name.setText(existing.getName());
            d.dosage.setText(existing.getDosage());
            d.switchAlters.setChecked(existing.altersCycle());
            d.switchReminder.setChecked(existing.isReminderEnabled());
            d.switchActive.setChecked(existing.isActive());
            d.notes.setText(existing.getNotes());
        } else {
            d.switchActive.setChecked(true);
        }
        Runnable updateTime = () -> {
            d.btnTime.setText(getString(R.string.meds_reminder_time, Formats.time(time[0], time[1])));
            d.btnTime.setEnabled(d.switchReminder.isChecked());
        };
        updateTime.run();
        d.switchReminder.setOnCheckedChangeListener((b, checked) -> {
            updateTime.run();
            if (checked && !NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                Snackbar.make(d.getRoot(), R.string.meds_notif_permission, Snackbar.LENGTH_LONG).show();
            }
        });
        d.btnTime.setOnClickListener(v -> DatePickers.pickTime(this, time[0], time[1], (h, m) -> {
            time[0] = h;
            time[1] = m;
            updateTime.run();
        }));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? R.string.meds_add : R.string.meds_edit)
                .setView(d.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);
        if (existing != null) {
            builder.setNeutralButton(R.string.delete, (dlg, w) -> new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getString(R.string.meds_delete_confirm, existing.getName()))
                    .setPositiveButton(R.string.delete, (x, y) -> vm.delete(existing))
                    .setNegativeButton(R.string.cancel, null)
                    .show());
        }
        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = text(d.name.getText());
            if (name == null) {
                d.nameLayout.setError(getString(R.string.meds_name_required));
                return;
            }
            vm.save(new Medication(existing != null ? existing.getId() : 0, name, text(d.dosage.getText()),
                    category[0], d.switchAlters.isChecked(), d.switchReminder.isChecked(), time[0], time[1],
                    d.switchActive.isChecked(), text(d.notes.getText())));
            dialog.dismiss();
        });
    }

    @Nullable
    private static String text(@Nullable CharSequence cs) {
        if (TextUtils.isEmpty(cs)) return null;
        String s = cs.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
