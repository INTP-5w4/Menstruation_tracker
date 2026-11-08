package com.example.andrs.di;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hilos compartidos: disco (serializado, evita condiciones de carrera) y red/respaldo. */
public final class AppExecutors {
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private final ExecutorService backup = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public Executor diskIO() { return diskIO; }
    public Executor backup() { return backup; }
    public Executor mainThread() { return mainHandler::post; }
}
