package com.example.andrs.domain.usecase;

import com.example.andrs.domain.engine.CycleEngine;
import com.example.andrs.domain.model.CycleSettings;
import com.example.andrs.domain.model.CycleStatus;
import com.example.andrs.domain.model.Period;

import java.time.LocalDate;
import java.util.List;

public class GetCycleStatusUseCase {
    private final CycleEngine engine;

    public GetCycleStatusUseCase(CycleEngine engine) {
        this.engine = engine;
    }

    public CycleStatus execute(List<Period> periods, CycleSettings settings, LocalDate today) {
        return engine.compute(periods, settings, today);
    }
}
