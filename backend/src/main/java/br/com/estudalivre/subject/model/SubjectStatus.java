package br.com.estudalivre.subject.model;

import java.util.Locale;

public enum SubjectStatus {
    ACTIVE,
    ARCHIVED;

    public static SubjectStatus from(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "active" -> ACTIVE;
            case "archived" -> ARCHIVED;
            default -> throw new IllegalArgumentException("O status deve ser active ou archived.");
        };
    }
}
