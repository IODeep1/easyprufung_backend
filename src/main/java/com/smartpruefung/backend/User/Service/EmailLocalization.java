package com.smartpruefung.backend.User.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/** Selects a complete subject/template pair without depending on the server's locale. */
public final class EmailLocalization {
    private static final Set<String> LANGUAGES = new HashSet<>(Arrays.asList("en", "de", "fr", "ar", "uk", "tr"));
    private final ClassLoader resources;

    public enum Kind {
        WELCOME("welcome", "email-welcome-template.flth", "Welcome to SmartPrüfung – Let’s Get Started!"),
        PASSWORD_RESET("password-reset", "email-reset-password-template.flth", "Password reset for SmartPrüfung.com account"),
        PURCHASE("purchase", "email-new_subscription-template.flth", "Your SmartPrüfung Purchase Is Confirmed");

        private final String key;
        private final String filename;
        private final String englishSubject;

        Kind(String key, String filename, String englishSubject) {
            this.key = key;
            this.filename = filename;
            this.englishSubject = englishSubject;
        }
    }

    public EmailLocalization() {
        this(EmailLocalization.class.getClassLoader());
    }

    public EmailLocalization(ClassLoader resources) {
        this.resources = resources;
    }

    public Selection resolve(Kind kind, String preferredLanguage) {
        String language = normalizeLanguage(preferredLanguage);
        String template = language + "/" + kind.filename;
        String subject = subject(language, kind.key);
        if (exists(template) && subject != null) {
            return new Selection(language, template, subject);
        }
        // Keep the English subject and body together when either translation is missing.
        String englishTemplate = "en/" + kind.filename;
        if (exists(englishTemplate)) {
            return new Selection("en", englishTemplate, kind.englishSubject);
        }
        // Original paths are retained as English templates for existing sender integrations.
        if (exists(kind.filename)) {
            return new Selection("en", kind.filename, kind.englishSubject);
        }
        throw new IllegalStateException("Missing English email template: " + kind.filename);
    }

    public static String normalizeLanguage(String value) {
        if (value == null) return "en";
        String language = value.trim().toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
        return LANGUAGES.contains(language) ? language : "en";
    }

    private boolean exists(String template) {
        return resources.getResource("templates/" + template) != null;
    }

    private String subject(String language, String key) {
        try (InputStream stream = resources.getResourceAsStream("templates/" + language + "/subjects.properties")) {
            if (stream == null) return null;
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String value = properties.getProperty(key);
            return value == null || value.trim().isEmpty() ? null : value;
        } catch (IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    public static final class Selection {
        private final String language;
        private final String template;
        private final String subject;

        private Selection(String language, String template, String subject) {
            this.language = language;
            this.template = template;
            this.subject = subject;
        }

        public String getLanguage() { return language; }
        public String getTemplate() { return template; }
        public String getSubject() { return subject; }
    }
}
