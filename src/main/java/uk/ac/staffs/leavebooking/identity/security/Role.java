package uk.ac.staffs.leavebooking.identity.security;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum Role {
    STAFF,
    MANAGER,
    HR,
    ADMIN;

    public static final String PREFIX = "ROLE_";

    public String getAuthority() {
        return PREFIX + name();
    }

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }

        String normalised = value.trim().toUpperCase(Locale.ROOT);
        if (normalised.startsWith(PREFIX)) {
            normalised = normalised.substring(PREFIX.length());
        }

        try {
            return Role.valueOf(normalised);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid role: " + value, exception);
        }
    }

    public static Role fromAuthority(String authority) {
        return fromString(authority);
    }
}
