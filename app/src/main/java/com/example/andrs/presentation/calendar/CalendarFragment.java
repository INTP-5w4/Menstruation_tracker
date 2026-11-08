package com.example.andrs.presentation.calendar;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.andrs.App;
import com.example.andrs.R;
import com.example.andrs.databinding.DialogEditPeriodBinding;
import com.example.andrs.databinding.FragmentCalendarBinding;
import com.example.andrs.domain.model.Period;
import com.example.andrs.presentation.common.DatePickers;
import com.example.andrs.presentation.common.Formats;
import com.example.andrs.presentation.common.UiMessage;
import com.example.andrs.presentation.common.ViewModelFactory;
import com.example.andrs.presentation.main.MainActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import android.content.res.ColorStateList;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class CalendarFragment extends Fragment {
    private FragmentCalendarBinding binding;
    private CalendarViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this, new ViewModelFactory(App.container(requireContext())))
                .get(CalendarViewModel.class);

        buildWeekdayHeader();
        buildLegend();

        CalendarAdapter adapter = new CalendarAdapter(this::onDayClicked);
        binding.grid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.grid.setAdapter(adapter);

        PeriodHistoryAdapter history = new PeriodHistoryAdapter(this::editPeriod);
        binding.history.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.history.setAdapter(history);

        binding.btnPrev.setOnClickListener(v -> vm.previousMonth());
        binding.btnNext.setOnClickListener(v -> vm.nextMonth());
        binding.btnToday.setOnClickListener(v -> vm.goToday());

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", Formats.locale());
        vm.getMonth().observe(getViewLifecycleOwner(), ym ->
                binding.monthTitle.setText(Formats.capitalize(ym.format(monthFmt))));
        vm.getDays().observe(getViewLifecycleOwner(), adapter::submit);
        vm.getHistory().observe(getViewLifecycleOwner(), rows -> {
            history.submit(rows);
            binding.historyEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.getStatus().observe(getViewLifecycleOwner(), s -> {
            binding.stats.setVisibility(s.hasData ? View.VISIBLE : View.GONE);
            binding.stats.setText(getString(R.string.cal_stats, s.cyclesUsed, s.variabilityDays));
        });
        vm.getMessages().observe(getViewLifecycleOwner(), e -> {
            UiMessage m = e.consume();
            if (m != null) Snackbar.make(binding.getRoot(), m.resolve(requireContext()), Snackbar.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void onDayClicked(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            Snackbar.make(binding.getRoot(), getString(R.string.cal_future_day, Formats.longDate(date)),
                    Snackbar.LENGTH_SHORT).show();
            return;
        }
        ((MainActivity) requireActivity()).openLog(date);
    }

    private void buildWeekdayHeader() {
        binding.weekdays.removeAllViews();
        for (DayOfWeek d : DayOfWeek.values()) {
            TextView tv = new TextView(requireContext());
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tv.setGravity(Gravity.CENTER);
            tv.setText(d.getDisplayName(TextStyle.NARROW, Formats.locale()));
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
            binding.weekdays.addView(tv);
        }
    }

    private void buildLegend() {
        addLegend(R.string.cal_legend_period, R.color.phase_menstrual);
        addLegend(R.string.cal_legend_predicted, R.color.day_predicted_period);
        addLegend(R.string.cal_legend_fertile, R.color.day_fertile);
        addLegend(R.string.cal_legend_ovulation, R.color.phase_ovulation);
    }

    private void addLegend(@StringRes int text, @ColorRes int color) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setClickable(false);
        chip.setChipIconResource(R.drawable.ic_drop);
        chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color)));
        chip.setChipIconVisible(true);
        binding.legend.addView(chip);
    }

    private void editPeriod(Period original) {
        final Period[] edited = {original};
        DialogEditPeriodBinding d = DialogEditPeriodBinding.inflate(getLayoutInflater());
        Runnable refresh = () -> {
            d.btnStart.setText(getString(R.string.cal_start) + ": " + Formats.mediumDate(edited[0].getStart()));
            d.btnEnd.setText(getString(R.string.cal_end) + ": " + (edited[0].getEnd() == null
                    ? getString(R.string.cal_no_end) : Formats.mediumDate(edited[0].getEnd())));
            d.btnClearEnd.setVisibility(edited[0].getEnd() == null ? View.GONE : View.VISIBLE);
        };
        refresh.run();
        d.btnStart.setOnClickListener(v -> DatePickers.pickPastDate(this, edited[0].getStart(), date -> {
            edited[0] = edited[0].withStart(date);
            refresh.run();
        }));
        d.btnEnd.setOnClickListener(v -> DatePickers.pickPastDate(this,
                edited[0].getEnd() != null ? edited[0].getEnd() : edited[0].getStart(), date -> {
                    edited[0] = edited[0].withEnd(date);
                    refresh.run();
                }));
        d.btnClearEnd.setOnClickListener(v -> {
            edited[0] = edited[0].withEnd(null);
            refresh.run();
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cal_edit_period)
                .setView(d.getRoot())
                .setPositiveButton(R.string.save, (dlg, w) -> vm.updatePeriod(edited[0]))
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete, (dlg, w) -> new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.cal_delete_confirm)
                        .setPositiveButton(R.string.delete, (x, y) -> vm.deletePeriod(original.getId()))
                        .setNegativeButton(R.string.cancel, null)
                        .show())
                .show();
    }
}
