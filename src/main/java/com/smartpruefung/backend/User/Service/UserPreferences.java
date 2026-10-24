package com.smartpruefung.backend.User.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Shared validation for registration and authenticated preference updates. */
public final class UserPreferences {
    private static final Set<String> LANGUAGES = new HashSet<>(Arrays.asList(
            "en", "de", "fr", "ar", "uk", "tr"));
    private static final Set<String> SOURCES = new HashSet<>(Arrays.asList(
            "google", "youtube", "facebook", "instagram", "tiktok", "reddit",
            "x", "linkedin", "whatsapp", "telegram", "discord", "other"));

    private UserPreferences() {}

    public static String language(String value) {
        if (value == null) return "en";
        String language = value.trim().toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
        if (!LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("UNSUPPORTED_LANGUAGE");
        }
        return language;
    }

    public static String acquisitionSource(String value) {
        if (value == null || !SOURCES.contains(value)) {
            throw new IllegalArgumentException("INVALID_ACQUISITION_SOURCE");
        }
        return value;
    }
}
