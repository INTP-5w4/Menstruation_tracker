package com.example.andrs.presentation.common;

import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.Consumer;

/** Selectores de fecha/hora de Material. MaterialDatePicker trabaja en milisegundos UTC. */
public final class DatePickers {
    private DatePickers() {}

    public interface TimeCallback {
        void onTime(int hour, int minute);
    }

    /** Selector limitado a hoy o fechas pasadas. */
    public static void pickPastDate(Fragment fragment, LocalDate initial, Consumer<LocalDate> onPicked) {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setEnd(toUtcMillis(LocalDate.now()))
                .setValidator(DateValidatorPointBackward.now())
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(toUtcMillis(initial))
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel != null) onPicked.accept(fromUtcMillis(sel));
        });
        picker.show(fragment.getChildFragmentManager(), "date_picker");
    }

    public static void pickTime(Fragment fragment, int hour, int minute, TimeCallback callback) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .build();
        picker.addOnPositiveButtonClickListener(v -> callback.onTime(picker.getHour(), picker.getMinute()));
        picker.show(fragment.getChildFragmentManager(), "time_picker");
    }

    static long toUtcMillis(LocalDate d) {
        return d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    static LocalDate fromUtcMillis(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
