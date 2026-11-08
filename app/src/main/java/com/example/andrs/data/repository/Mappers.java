package com.example.andrs.data.repository;

import androidx.annotation.Nullable;

import com.example.andrs.data.local.entity.DailyLogEntity;
import com.example.andrs.data.local.entity.IntakeWithMedication;
import com.example.andrs.data.local.entity.IntimacyEntity;
import com.example.andrs.data.local.entity.MedicationEntity;
import com.example.andrs.data.local.entity.PeriodEntity;
import com.example.andrs.domain.model.DailyLog;
import com.example.andrs.domain.model.FlowLevel;
import com.example.andrs.domain.model.IntimacyLog;
import com.example.andrs.domain.model.Medication;
import com.example.andrs.domain.model.MedicationCategory;
import com.example.andrs.domain.model.MedicationIntake;
import com.example.andrs.domain.model.Mood;
import com.example.andrs.domain.model.Period;
import com.example.andrs.domain.model.Protection;
import com.example.andrs.domain.model.Symptom;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Conversión entre entidades de Room y modelos de dominio. */
final class Mappers {
    private Mappers() {}

    static Period toDomain(PeriodEntity e) {
        return new Period(e.id, LocalDate.ofEpochDay(e.startDay),
                e.endDay == null ? null : LocalDate.ofEpochDay(e.endDay), e.notes);
    }

    static PeriodEntity toEntity(Period p) {
        PeriodEntity e = new PeriodEntity();
        e.id = p.getId();
        e.startDay = p.getStart().toEpochDay();
        e.endDay = p.getEnd() == null ? null : p.getEnd().toEpochDay();
        e.notes = p.getNotes();
        return e;
    }

    static List<Period> periods(@Nullable List<PeriodEntity> list) {
        List<Period> out = new ArrayList<>();
        if (list != null) for (PeriodEntity e : list) out.add(toDomain(e));
        return out;
    }

    @Nullable
    static DailyLog toDomain(@Nullable DailyLogEntity e) {
        if (e == null) return null;
        Set<Symptom> symptoms = EnumSet.noneOf(Symptom.class);
        for (String code : e.symptoms.split(",")) {
            Symptom s = parse(Symptom.class, code.trim(), null);
            if (s != null) symptoms.add(s);
        }
        return new DailyLog(LocalDate.ofEpochDay(e.day),
                parse(FlowLevel.class, e.flow, FlowLevel.NONE), symptoms,
                parse(Mood.class, e.mood, Mood.NONE), e.notes);
    }

    static DailyLogEntity toEntity(DailyLog log) {
        DailyLogEntity e = new DailyLogEntity();
        e.day = log.getDate().toEpochDay();
        e.flow = log.getFlow().name();
        StringBuilder sb = new StringBuilder();
        for (Symptom s : log.getSymptoms()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(s.name());
        }
        e.symptoms = sb.toString();
        e.mood = log.getMood().name();
        e.notes = log.getNotes();
        return e;
    }

    @Nullable
    static Medication toDomain(@Nullable MedicationEntity e) {
        if (e == null) return null;
        return new Medication(e.id, e.name, e.dosage,
                parse(MedicationCategory.class, e.category, MedicationCategory.OTHER),
                e.altersCycle, e.reminderEnabled, e.reminderHour, e.reminderMinute, e.active, e.notes);
    }

    static MedicationEntity toEntity(Medication m) {
        MedicationEntity e = new MedicationEntity();
        e.id = m.getId();
        e.name = m.getName();
        e.dosage = m.getDosage();
        e.category = m.getCategory().name();
        e.altersCycle = m.altersCycle();
        e.reminderEnabled = m.isReminderEnabled();
        e.reminderHour = m.getReminderHour();
        e.reminderMinute = m.getReminderMinute();
        e.active = m.isActive();
        e.notes = m.getNotes();
        return e;
    }

    static MedicationIntake toDomain(IntakeWithMedication e) {
        return new MedicationIntake(e.id, e.medicationId, e.medicationName, e.altersCycle,
                fromMillis(e.takenAt), e.notes);
    }

    static IntimacyLog toDomain(IntimacyEntity e) {
        return new IntimacyLog(e.id, LocalDate.ofEpochDay(e.day),
                parse(Protection.class, e.protection, Protection.NONE), e.notes);
    }

    static IntimacyEntity toEntity(IntimacyLog l) {
        IntimacyEntity e = new IntimacyEntity();
        e.id = l.getId();
        e.day = l.getDate().toEpochDay();
        e.protection = l.getProtection().name();
        e.notes = l.getNotes();
        return e;
    }

    static long toMillis(LocalDateTime dt) {
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    static LocalDateTime fromMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    @Nullable
    private static <E extends Enum<E>> E parse(Class<E> type, @Nullable String value, @Nullable E fallback) {
        if (value == null || value.isEmpty()) return fallback;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
