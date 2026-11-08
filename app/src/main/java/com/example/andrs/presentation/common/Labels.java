package com.example.andrs.presentation.common;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import com.example.andrs.R;
import com.example.andrs.domain.model.Confidence;
import com.example.andrs.domain.model.CyclePhase;
import com.example.andrs.domain.model.FlowLevel;
import com.example.andrs.domain.model.MedicationCategory;
import com.example.andrs.domain.model.Mood;
import com.example.andrs.domain.model.Protection;
import com.example.andrs.domain.model.Symptom;

/** Traducción de enums del dominio a textos y colores de la UI. */
public final class Labels {
    private Labels() {}

    @StringRes
    public static int phase(CyclePhase p) {
        switch (p) {
            case MENSTRUAL: return R.string.phase_menstrual;
            case FOLLICULAR: return R.string.phase_follicular;
            case OVULATION: return R.string.phase_ovulation;
            case LUTEAL: return R.string.phase_luteal;
            default: return R.string.phase_unknown;
        }
    }

    @StringRes
    public static int phaseDescription(CyclePhase p) {
        switch (p) {
            case MENSTRUAL: return R.string.phase_menstrual_desc;
            case FOLLICULAR: return R.string.phase_follicular_desc;
            case OVULATION: return R.string.phase_ovulation_desc;
            case LUTEAL: return R.string.phase_luteal_desc;
            default: return R.string.phase_unknown_desc;
        }
    }

    @ColorRes
    public static int phaseColor(CyclePhase p) {
        switch (p) {
            case MENSTRUAL: return R.color.phase_menstrual;
            case FOLLICULAR: return R.color.phase_follicular;
            case OVULATION: return R.color.phase_ovulation;
            case LUTEAL: return R.color.phase_luteal;
            default: return R.color.phase_unknown;
        }
    }

    @StringRes
    public static int confidence(Confidence c) {
        switch (c) {
            case HIGH: return R.string.confidence_high;
            case MEDIUM: return R.string.confidence_medium;
            default: return R.string.confidence_low;
        }
    }

    @StringRes
    public static int flow(FlowLevel f) {
        switch (f) {
            case SPOTTING: return R.string.flow_spotting;
            case LIGHT: return R.string.flow_light;
            case MEDIUM: return R.string.flow_medium;
            case HEAVY: return R.string.flow_heavy;
            default: return R.string.flow_none;
        }
    }

    @StringRes
    public static int mood(Mood m) {
        switch (m) {
            case HAPPY: return R.string.mood_happy;
            case CALM: return R.string.mood_calm;
            case ENERGETIC: return R.string.mood_energetic;
            case SENSITIVE: return R.string.mood_sensitive;
            case SAD: return R.string.mood_sad;
            case IRRITABLE: return R.string.mood_irritable;
            case ANXIOUS: return R.string.mood_anxious;
            default: return R.string.mood_none;
        }
    }

    @StringRes
    public static int symptom(Symptom s) {
        switch (s) {
            case CRAMPS: return R.string.sym_cramps;
            case HEADACHE: return R.string.sym_headache;
            case BLOATING: return R.string.sym_bloating;
            case BREAST_TENDERNESS: return R.string.sym_breast;
            case BACK_PAIN: return R.string.sym_back;
            case FATIGUE: return R.string.sym_fatigue;
            case ACNE: return R.string.sym_acne;
            case NAUSEA: return R.string.sym_nausea;
            case CRAVINGS: return R.string.sym_cravings;
            case INSOMNIA: return R.string.sym_insomnia;
            case DIARRHEA: return R.string.sym_diarrhea;
            case CONSTIPATION: return R.string.sym_constipation;
            case DISCHARGE: return R.string.sym_discharge;
            case MOOD_SWINGS: return R.string.sym_mood_swings;
            case DIZZINESS: return R.string.sym_dizziness;
            case HOT_FLASHES: return R.string.sym_hot_flashes;
            case LOW_LIBIDO: return R.string.sym_low_libido;
            case HIGH_LIBIDO: return R.string.sym_high_libido;
            default: throw new IllegalArgumentException(s.name());
        }
    }

    @StringRes
    public static int protection(Protection p) {
        switch (p) {
            case CONDOM: return R.string.protection_condom;
            case HORMONAL: return R.string.protection_hormonal;
            case OTHER: return R.string.protection_other;
            default: return R.string.protection_none;
        }
    }

    @StringRes
    public static int category(MedicationCategory c) {
        switch (c) {
            case HORMONAL_CONTRACEPTIVE: return R.string.cat_hormonal_contraceptive;
            case EMERGENCY_CONTRACEPTIVE: return R.string.cat_emergency_contraceptive;
            case HORMONE_THERAPY: return R.string.cat_hormone_therapy;
            case PAIN_RELIEF: return R.string.cat_pain_relief;
            case ANTIBIOTIC: return R.string.cat_antibiotic;
            case SUPPLEMENT: return R.string.cat_supplement;
            default: return R.string.cat_other;
        }
    }
}
