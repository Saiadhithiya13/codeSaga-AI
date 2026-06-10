package com.codesage.common.util;

import java.util.UUID;

/**
 * General-purpose string utility methods for CodeSage AI.
 *
 * <p>All methods are null-safe and do not throw on null input.
 */
public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns {@code true} if the string is null or blank.
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Returns {@code true} if the string is non-null and non-blank.
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /**
     * Truncates a string to the specified max length, appending "..." if truncated.
     *
     * @param value     the string to truncate
     * @param maxLength the maximum character length
     * @return truncated string or original if shorter than {@code maxLength}
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Generates a new random UUID string (no dashes).
     * Useful for short identifiers.
     */
    public static String randomShortId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Converts a string to snake_case from camelCase.
     * Example: {@code "myFieldName"} → {@code "my_field_name"}
     */
    public static String toSnakeCase(String camelCase) {
        if (isBlank(camelCase)) return camelCase;
        return camelCase
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
    }

    /**
     * Masks a secret string, showing only the last 4 characters.
     * Example: {@code "sk-abc12345"} → {@code "****2345"}
     *
     * @param secret the secret to mask
     * @return masked string safe for logging
     */
    public static String maskSecret(String secret) {
        if (isBlank(secret)) return "****";
        int len = secret.length();
        if (len <= 4) return "****";
        return "*".repeat(len - 4) + secret.substring(len - 4);
    }
}
