package com.example.andrs.presentation.home;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.databinding.FragmentHomeBinding;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.model.DailyLog;
import com.example.andrs.domain.model.Symptom;
import com.example.andrs.presentation.common.DatePickers;
import com.example.andrs.presentation.common.Formats;
import com.example.andrs.presentation.common.Labels;
import com.example.andrs.presentation.common.UiMessage;
import com.example.andrs.presentation.common.ViewModelFactory;
import com.example.andrs.presentation.main.MainActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel vm;
    private CycleStatus lastStatus;
    private boolean hormonal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this, new ViewModelFactory(App.container(requireContext())))
                .get(HomeViewModel.class);

        binding.dateTitle.setText(Formats.longDate(LocalDate.now()));
        vm.getStatus().observe(getViewLifecycleOwner(), this::render);
        vm.getHormonalWarning().observe(getViewLifecycleOwner(), w -> {
            hormonal = Boolean.TRUE.equals(w);
            if (lastStatus != null) renderWarnings(lastStatus);
        });
        vm.getTodayLog().observe(getViewLifecycleOwner(), this::renderTodayLog);
        vm.getMessages().observe(getViewLifecycleOwner(), e -> {
            UiMessage m = e.consume();
            if (m != null) Snackbar.make(binding.getRoot(), m.resolve(requireContext()), Snackbar.LENGTH_SHORT).show();
        });

        binding.btnPeriodToday.setOnClickListener(v -> {
            if (lastStatus != null && lastStatus.periodOngoing) vm.endPeriod(LocalDate.now());
            else vm.startPeriod(LocalDate.now());
        });
        binding.btnOtherDate.setOnClickListener(v -> pickOtherDate());
        binding.btnLogToday.setOnClickListener(v -> ((MainActivity) requireActivity()).openLog(LocalDate.now()));
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && vm != null) vm.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void render(CycleStatus s) {
        lastStatus = s;
        int phaseColor = ContextCompat.getColor(requireContext(), Labels.phaseColor(s.phase));
        String phaseName = getString(Labels.phase(s.phase));

        if (!s.hasData) {
            binding.ring.setCycle(s.averageCycleLength, s.averagePeriodLength, 0, 0, "?", phaseName);
            binding.phaseName.setText(R.string.home_empty_title);
            binding.phaseDesc.setText(R.string.home_empty_text);
            binding.predictionCard.setVisibility(View.GONE);
            binding.confidenceChip.setVisibility(View.GONE);
            binding.btnPeriodToday.setText(R.string.home_btn_period_started);
            renderWarnings(s);
            return;
        }

        int ovulationDay = s.ovulationDate == null ? s.averageCycleLength - 14
                : (int) ChronoUnit.DAYS.between(s.lastPeriodStart, s.ovulationDate) + 1;
        binding.ring.setCycle(s.averageCycleLength, s.averagePeriodLength, ovulationDay, s.cycleDay,
                getString(R.string.home_cycle_day, s.cycleDay), phaseName);

        binding.phaseName.setText(getString(R.string.home_phase_label, phaseName));
        binding.phaseName.setTextColor(phaseColor);
        binding.phaseDesc.setText(Labels.phaseDescription(s.phase));

        binding.confidenceChip.setVisibility(View.VISIBLE);
        binding.confidenceChip.setText(Labels.confidence(s.confidence));

        binding.predictionCard.setVisibility(View.VISIBLE);
        binding.nextPeriodDate.setText(Formats.longDate(s.nextPeriodStart));
        String relative;
        if (s.periodOngoing) {
            relative = getString(R.string.home_period_ongoing, s.cycleDay);
        } else if (s.isLate) {
            relative = getString(R.string.home_late, Formats.days(requireContext(), s.daysLate));
        } else if (s.daysUntilNextPeriod == 0) {
            relative = getString(R.string.home_today);
        } else {
            relative = getString(R.string.home_in_days, Formats.days(requireContext(), s.daysUntilNextPeriod));
        }
        binding.nextPeriodRelative.setText(relative);
        binding.fertileWindow.setText(Formats.range(s.fertileStart, s.fertileEnd));
        binding.ovulationDate.setText(Formats.shortDate(s.ovulationDate));
        binding.averages.setText(s.cyclesUsed > 0
                ? getString(R.string.home_averages, s.averageCycleLength, s.averagePeriodLength)
                : getString(R.string.home_averages_default, s.averageCycleLength, s.averagePeriodLength));

        binding.btnPeriodToday.setText(s.periodOngoing ? R.string.home_btn_period_ended : R.string.home_btn_period_started);
        binding.btnPeriodToday.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.phase_menstrual)));
        renderWarnings(s);
    }

    private void renderWarnings(CycleStatus s) {
        binding.warnings.removeAllViews();
        if (hormonal) addWarning(getString(R.string.home_warning_meds));
        if (s.hasData && s.irregular) addWarning(getString(R.string.home_warning_irregular, s.variabilityDays));
        if (s.hasData && s.isLate && s.daysLate >= 5) {
            addWarning(getString(R.string.home_warning_late, Formats.days(requireContext(), s.daysLate)));
        }
    }

    private void addWarning(String text) {
        View v = getLayoutInflater().inflate(R.layout.view_warning, binding.warnings, false);
        ((TextView) v.findViewById(R.id.text)).setText(text);
        binding.warnings.addView(v);
    }

    private void renderTodayLog(@Nullable DailyLog log) {
        if (log == null || log.isEmpty()) {
            binding.todaySummary.setText(R.string.home_today_none);
            return;
        }
        List<String> parts = new ArrayList<>();
        if (log.getFlow() != com.example.andrs.domain.model.FlowLevel.NONE) parts.add(getString(Labels.flow(log.getFlow())));
        for (Symptom s : log.getSymptoms()) parts.add(getString(Labels.symptom(s)));
        if (log.getMood() != com.example.andrs.domain.model.Mood.NONE) parts.add(getString(Labels.mood(log.getMood())));
        binding.todaySummary.setText(parts.isEmpty() ? getString(R.string.home_today_none)
                : getString(R.string.home_today_summary, String.join(", ", parts)));
    }

    private void pickOtherDate() {
        DatePickers.pickPastDate(this, LocalDate.now(), date -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.home_which_event, Formats.longDate(date)))
                .setPositiveButton(R.string.home_event_started, (d, w) -> vm.startPeriod(date))
                .setNegativeButton(R.string.home_event_ended, (d, w) -> vm.endPeriod(date))
                .setNeutralButton(R.string.cancel, null)
                .show());
    }
}
