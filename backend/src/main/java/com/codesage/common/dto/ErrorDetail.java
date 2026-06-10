package com.codesage.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single field-level validation error.
 *
 * <p>Used inside {@link ApiResponse} when returning validation failures
 * from {@code @Valid} annotated request bodies. Each instance corresponds
 * to one failed constraint on one field.
 *
 * @param field   the name of the field that failed validation
 * @param message the constraint violation message
 * @param rejectedValue the value that was rejected (as a string for JSON safety)
 */
@Schema(description = "Field-level validation error detail")
public record ErrorDetail(

        @Schema(description = "Field name that failed validation", example = "email")
        String field,

        @Schema(description = "Constraint violation message", example = "must be a valid email address")
        String message,

        @Schema(description = "The rejected value", example = "not-an-email")
        String rejectedValue
) {

    public static ErrorDetail of(String field, String message, Object rejectedValue) {
        return new ErrorDetail(field, message, rejectedValue != null ? rejectedValue.toString() : null);
    }
}
