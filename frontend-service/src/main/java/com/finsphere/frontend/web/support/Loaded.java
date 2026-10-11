package com.finsphere.frontend.web.support;

import org.jspecify.annotations.Nullable;

/**
 * Result of loading one section of a page: the value, or the reason it could not be loaded. Lets a page render
 * the sections that worked when one service is slow or down.
 */
public record Loaded<T>(@Nullable T value, @Nullable String error) {

    public static <T> Loaded<T> of(T value) {
        return new Loaded<>(value, null);
    }

    public static <T> Loaded<T> failed(String error) {
        return new Loaded<>(null, error);
    }

    public boolean ok() {
        return this.error == null;
    }
}
