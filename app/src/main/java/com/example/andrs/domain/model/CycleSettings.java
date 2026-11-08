package com.example.andrs.domain.model;

/** Valores por defecto del usuario, usados cuando aún no hay historial suficiente. */
public final class CycleSettings {
    public static final int DEFAULT_CYCLE_LENGTH = 28;
    public static final int DEFAULT_PERIOD_LENGTH = 5;
    public static final int DEFAULT_LUTEAL_LENGTH = 14;

    private final int cycleLength;
    private final int periodLength;
    private final int lutealLength;

    public CycleSettings(int cycleLength, int periodLength, int lutealLength) {
        this.cycleLength = cycleLength;
        this.periodLength = periodLength;
        this.lutealLength = lutealLength;
    }

    public static CycleSettings defaults() {
        return new CycleSettings(DEFAULT_CYCLE_LENGTH, DEFAULT_PERIOD_LENGTH, DEFAULT_LUTEAL_LENGTH);
    }

    public int getCycleLength() { return cycleLength; }
    public int getPeriodLength() { return periodLength; }
    public int getLutealLength() { return lutealLength; }
}
