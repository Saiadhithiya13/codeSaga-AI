package com.codesage.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date/time operations.
 *
 * <p>All timestamps in CodeSage AI are stored and returned as UTC.
 * Architecture Spec mandates TIMESTAMPTZ everywhere.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    /**
     * Returns the current UTC timestamp as an {@link Instant}.
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Formats an {@link Instant} to ISO-8601 UTC string.
     *
     * @param instant the instant to format
     * @return formatted string e.g. {@code "2025-01-15T10:30:00Z"}
     */
    public static String formatIso(Instant instant) {
        if (instant == null) return null;
        return ISO_FORMATTER.format(instant);
    }

    /**
     * Formats an {@link Instant} to date-only string.
     *
     * @param instant the instant to format
     * @return formatted string e.g. {@code "2025-01-15"}
     */
    public static String formatDate(Instant instant) {
        if (instant == null) return null;
        return DATE_FORMATTER.format(instant);
    }

    /**
     * Converts a {@link LocalDateTime} (assumed UTC) to an {@link Instant}.
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}
