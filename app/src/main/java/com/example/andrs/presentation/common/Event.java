package com.example.andrs.presentation.common;

import androidx.annotation.Nullable;

/** Valor de un solo consumo para LiveData (mensajes, navegación). */
public final class Event<T> {
    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    @Nullable
    public T consume() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
