package com.codesage.common.constants;

/**
 * Application-wide constants.
 *
 * <p>Centralizes magic strings and values to eliminate duplication.
 * All values here are truly constant and do NOT represent configuration.
 * For environment-based config, see {@code application.yml}.
 */
public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ─── API ───────────────────────────────────────────────────────────────
    public static final String API_BASE_PATH      = "/api/v1";
    public static final String HEALTH_PATH         = "/health";
    public static final String ACTUATOR_PATH       = "/actuator/**";
    public static final String SWAGGER_UI_PATH     = "/swagger-ui/**";
    public static final String API_DOCS_PATH       = "/v3/api-docs/**";

    // ─── Pagination defaults ───────────────────────────────────────────────
    public static final int DEFAULT_PAGE_SIZE      = 20;
    public static final int MAX_PAGE_SIZE          = 100;

    // ─── Date / Time ──────────────────────────────────────────────────────
    public static final String DATE_TIME_FORMAT    = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DATE_FORMAT         = "yyyy-MM-dd";

    // ─── Logging ──────────────────────────────────────────────────────────
    public static final String LOG_CORRELATION_ID  = "correlationId";
    public static final String LOG_REQUEST_PATH    = "requestPath";

    // ─── Response messages ────────────────────────────────────────────────
    public static final String MSG_SUCCESS         = "Success";
    public static final String MSG_NOT_FOUND       = "Resource not found";
    public static final String MSG_VALIDATION_ERR  = "Validation failed";
    public static final String MSG_INTERNAL_ERR    = "An unexpected error occurred";
    public static final String MSG_EXTERNAL_ERR    = "External service error";
}
