package com.example.andrs.presentation.common;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.andrs.R;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Formatos de fecha localizados. */
public final class Formats {
    private Formats() {}

    /** Los textos de la app están en español: las fechas también, aunque el sistema use otro idioma. */
    public static Locale locale() {
        Locale def = Locale.getDefault();
        return "es".equals(def.getLanguage()) ? def : new Locale("es");
    }

    /** "8 nov" */
    public static String shortDate(@Nullable LocalDate d) {
        if (d == null) return "—";
        return d.format(DateTimeFormatter.ofPattern("d MMM", locale()));
    }

    /** "sábado, 8 de noviembre" */
    public static String longDate(LocalDate d) {
        return capitalize(d.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale())));
    }

    /** "8 nov 2026" */
    public static String mediumDate(LocalDate d) {
        return d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()));
    }

    public static String dateTime(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale()));
    }

    public static String time(int hour, int minute) {
        return String.format(locale(), "%02d:%02d", hour, minute);
    }

    public static String range(@Nullable LocalDate a, @Nullable LocalDate b) {
        return shortDate(a) + " – " + shortDate(b);
    }

    public static String days(Context c, int n) {
        return c.getResources().getQuantityString(R.plurals.days, n, n);
    }

    public static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(locale()) + s.substring(1);
    }
}
