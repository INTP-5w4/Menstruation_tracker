package com.example.andrs.domain.model;

/** Categoría del medicamento; algunas alteran el ciclo por defecto. */
public enum MedicationCategory {
    HORMONAL_CONTRACEPTIVE(true),
    EMERGENCY_CONTRACEPTIVE(true),
    HORMONE_THERAPY(true),
    PAIN_RELIEF(false),
    ANTIBIOTIC(false),
    SUPPLEMENT(false),
    OTHER(false);

    private final boolean altersCycleByDefault;

    MedicationCategory(boolean altersCycleByDefault) {
        this.altersCycleByDefault = altersCycleByDefault;
    }

    public boolean altersCycleByDefault() {
        return altersCycleByDefault;
    }
}
