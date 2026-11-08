package com.example.andrs.presentation.log;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.databinding.DialogIntimacyBinding;
import com.example.andrs.databinding.FragmentLogBinding;
import com.example.andrs.databinding.ItemIntimacyBinding;
import com.example.andrs.domain.model.DailyLog;
import com.example.andrs.domain.model.DayType;
import com.example.andrs.domain.model.FlowLevel;
import com.example.andrs.domain.model.IntimacyLog;
import com.example.andrs.domain.model.Mood;
import com.example.andrs.domain.model.Protection;
import com.example.andrs.domain.model.Symptom;
import com.example.andrs.presentation.common.DatePickers;
import com.example.andrs.presentation.common.Formats;
import com.example.andrs.presentation.common.Labels;
import com.example.andrs.presentation.common.UiMessage;
import com.example.andrs.presentation.common.ViewModelFactory;
import com.example.andrs.presentation.main.MainViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogFragment extends Fragment {
    private FragmentLogBinding binding;
    private LogViewModel vm;
    private MainViewModel mainVm;

    private final Map<FlowLevel, Chip> flowChips = new EnumMap<>(FlowLevel.class);
    private final Map<Symptom, Chip> symptomChips = new EnumMap<>(Symptom.class);
    private final Map<Mood, Chip> moodChips = new EnumMap<>(Mood.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewModelFactory factory = new ViewModelFactory(App.container(requireContext()));
        vm = new ViewModelProvider(this, factory).get(LogViewModel.class);
        mainVm = new ViewModelProvider(requireActivity(), factory).get(MainViewModel.class);

        buildChips();

        mainVm.getLogDate().observe(getViewLifecycleOwner(), vm::setDate);
        vm.getDate().observe(getViewLifecycleOwner(), d -> {
            binding.btnDate.setText(d.equals(LocalDate.now()) ? getString(R.string.today) : Formats.longDate(d));
            binding.btnNextDay.setEnabled(d.isBefore(LocalDate.now()));
        });
        vm.getLog().observe(getViewLifecycleOwner(), this::populate);
        vm.getIntimacy().observe(getViewLifecycleOwner(), this::renderIntimacy);
        vm.getDayType().observe(getViewLifecycleOwner(), t -> binding.fertileHint.setVisibility(
                t == DayType.FERTILE || t == DayType.OVULATION ? View.VISIBLE : View.GONE));
        vm.getMessages().observe(getViewLifecycleOwner(), e -> {
            UiMessage m = e.consume();
            if (m != null) Snackbar.make(binding.getRoot(), m.resolve(requireContext()), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.fabSave).show();
        });
        vm.getSignals().observe(getViewLifecycleOwner(), e -> {
            LogViewModel.Signal s = e.consume();
            if (s == LogViewModel.Signal.SUGGEST_PERIOD_START) {
                Snackbar.make(binding.getRoot(), R.string.log_suggest_start, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.fabSave)
                        .setAction(R.string.log_mark, v -> vm.markPeriodStart()).show();
            } else if (s == LogViewModel.Signal.FERTILE_UNPROTECTED) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.intimacy_fertile_title)
                        .setMessage(R.string.intimacy_fertile_text)
                        .setPositiveButton(R.string.accept, null)
                        .show();
            }
        });

        binding.btnPrevDay.setOnClickListener(v -> mainVm.setLogDate(current().minusDays(1)));
        binding.btnNextDay.setOnClickListener(v -> mainVm.setLogDate(current().plusDays(1)));
        binding.btnDate.setOnClickListener(v -> DatePickers.pickPastDate(this, current(), mainVm::setLogDate));
        binding.fabSave.setOnClickListener(v -> vm.save(collect()));
        binding.btnAddIntimacy.setOnClickListener(v -> showIntimacyDialog());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        flowChips.clear();
        symptomChips.clear();
        moodChips.clear();
    }

    private LocalDate current() {
        LocalDate d = vm.getDate().getValue();
        return d != null ? d : LocalDate.now();
    }

    private void buildChips() {
        for (FlowLevel f : FlowLevel.values()) {
            flowChips.put(f, addChip(binding.flowGroup, getString(Labels.flow(f))));
        }
        for (Symptom s : Symptom.values()) {
            symptomChips.put(s, addChip(binding.symptomGroup, getString(Labels.symptom(s))));
        }
        for (Mood m : Mood.values()) {
            if (m == Mood.NONE) continue;
            moodChips.put(m, addChip(binding.moodGroup, getString(Labels.mood(m))));
        }
    }

    private Chip addChip(ChipGroup group, String text) {
        Chip chip = (Chip) getLayoutInflater().inflate(
                R.layout.item_chip_filter, group, false);
        chip.setText(text);
        chip.setId(View.generateViewId());
        group.addView(chip);
        return chip;
    }

    private void populate(@Nullable DailyLog log) {
        DailyLog l = log != null ? log : DailyLog.empty(current());
        for (Map.Entry<FlowLevel, Chip> e : flowChips.entrySet()) e.getValue().setChecked(e.getKey() == l.getFlow());
        for (Map.Entry<Symptom, Chip> e : symptomChips.entrySet()) e.getValue().setChecked(l.getSymptoms().contains(e.getKey()));
        for (Map.Entry<Mood, Chip> e : moodChips.entrySet()) e.getValue().setChecked(e.getKey() == l.getMood());
        binding.notes.setText(l.getNotes());
    }

    private DailyLog collect() {
        FlowLevel flow = FlowLevel.NONE;
        for (Map.Entry<FlowLevel, Chip> e : flowChips.entrySet()) if (e.getValue().isChecked()) flow = e.getKey();
        Set<Symptom> symptoms = EnumSet.noneOf(Symptom.class);
        for (Map.Entry<Symptom, Chip> e : symptomChips.entrySet()) if (e.getValue().isChecked()) symptoms.add(e.getKey());
        Mood mood = Mood.NONE;
        for (Map.Entry<Mood, Chip> e : moodChips.entrySet()) if (e.getValue().isChecked()) mood = e.getKey();
        CharSequence notes = binding.notes.getText();
        String n = TextUtils.isEmpty(notes) ? null : notes.toString().trim();
        return new DailyLog(current(), flow, symptoms, mood, n);
    }

    private void renderIntimacy(List<IntimacyLog> list) {
        binding.intimacyList.removeAllViews();
        binding.intimacyEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        if (list == null) return;
        for (IntimacyLog l : list) {
            ItemIntimacyBinding item = ItemIntimacyBinding.inflate(getLayoutInflater(), binding.intimacyList, false);
            item.protection.setText(Labels.protection(l.getProtection()));
            item.notes.setText(l.getNotes());
            item.notes.setVisibility(TextUtils.isEmpty(l.getNotes()) ? View.GONE : View.VISIBLE);
            item.btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.intimacy_delete_confirm)
                    .setPositiveButton(R.string.delete, (d, w) -> vm.deleteIntimacy(l.getId()))
                    .setNegativeButton(R.string.cancel, null)
                    .show());
            binding.intimacyList.addView(item.getRoot());
        }
    }

    private void showIntimacyDialog() {
        DialogIntimacyBinding d = DialogIntimacyBinding.inflate(getLayoutInflater());
        Map<Integer, Protection> ids = new java.util.HashMap<>();
        for (Protection p : Protection.values()) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setId(View.generateViewId());
            rb.setText(Labels.protection(p));
            ids.put(rb.getId(), p);
            d.protectionGroup.addView(rb);
            if (p == Protection.CONDOM) rb.setChecked(true);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.intimacy_dialog_title)
                .setView(d.getRoot())
                .setPositiveButton(R.string.save, (dlg, w) -> {
                    Protection p = ids.get(d.protectionGroup.getCheckedRadioButtonId());
                    CharSequence n = d.notes.getText();
                    vm.addIntimacy(p != null ? p : Protection.NONE,
                            TextUtils.isEmpty(n) ? null : n.toString().trim());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
