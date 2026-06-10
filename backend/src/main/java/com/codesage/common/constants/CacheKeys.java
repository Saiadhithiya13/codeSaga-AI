package com.codesage.common.constants;

/**
 * Redis cache key patterns for CodeSage AI.
 *
 * <p>All keys follow the pattern: {@code domain:entity:{id}:qualifier}
 * to enable namespace-based invalidation and easy monitoring.
 *
 * <p>Use {@link String#formatted(Object...)} to substitute IDs at runtime:
 * <pre>{@code
 *   String key = CacheKeys.GITHUB_USER_REPOS.formatted(userId);
 * }</pre>
 */
public final class CacheKeys {

    private CacheKeys() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ─── GitHub ────────────────────────────────────────────────────────────
    /** Cached list of repos for a GitHub user. TTL: 5 min */
    public static final String GITHUB_USER_REPOS        = "github:user:%s:repos";

    /** Cached repository metadata. TTL: 10 min */
    public static final String GITHUB_REPO_METADATA     = "github:repo:%s:metadata";

    // ─── Analytics ────────────────────────────────────────────────────────
    /** Latest analytics snapshot for a repository. TTL: 15 min */
    public static final String ANALYTICS_REPO_LATEST    = "analytics:repo:%s:latest";

    // ─── Session / Auth ───────────────────────────────────────────────────
    /** Refresh token entry for a user. TTL: 7 days */
    public static final String SESSION_REFRESH_TOKEN    = "session:refresh:%s";

    // ─── Rate Limiting ────────────────────────────────────────────────────
    /** API rate limit counter per user. TTL: 1 min (sliding window) */
    public static final String RATE_LIMIT_API           = "rate_limit:api:%s";

    // ─── Repository Ingestion ─────────────────────────────────────────────
    /** Distributed lock for repository indexing. TTL: 30 min */
    public static final String REPO_INDEXING_LOCK       = "repo:indexing_lock:%s";
}
