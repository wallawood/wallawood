package io.gemboot;

import io.gemboot.annotations.RequireAuthorized;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * An immutable authorization requirement that can be checked against a
 * {@link Grant}. Use this in {@link RequestInterceptor @Preprocessor}
 * interceptors to perform the same checks the framework applies for
 * {@link RequireAuthorized @RequireAuthorized},
 * {@link io.gemboot.annotations.RequireClearance @RequireClearance}, and
 * {@link io.gemboot.annotations.RequireScopes @RequireScopes}.
 *
 * <p>Each requirement checks a single dimension of the grant. Use
 * {@link #builder()} to combine multiple requirements with AND semantics.
 * For OR logic, use standard Java {@code ||} on multiple {@code check()} calls.
 *
 * <pre>{@code
 * // Single-dimension check:
 * private static final Authorization MOD = Authorization.requireClearance(3);
 *
 * // Multi-dimension check (AND logic):
 * private static final Authorization STRICT = Authorization.builder()
 *     .requireAuthorized()
 *     .requireClearance(3)
 *     .requireScopes("ban_users", "delete_posts")
 *     .build();
 *
 * if (!STRICT.check(grant)) {
 *     return Optional.of(GeminiResponse.certificateNotAuthorized("Forbidden."));
 * }
 * }</pre>
 *
 * @see Grant
 * @see RequireAuthorized
 * @see io.gemboot.annotations.RequireClearance
 * @see io.gemboot.annotations.RequireScopes
 */
public final class Authorization {
    /** Default message for failed authorization checks. */
    public static final String DEFAULT_MESSAGE = "Access Denied";

    private final Predicate<Grant> checker;

    private Authorization(Predicate<Grant> checker) {
        this.checker = checker;
    }

    /**
     * Creates a new Builder for constructing a composite Authorization (AND logic).
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Requires the grant to explicitly represent an authenticated/authorized state.
     *
     * @return an authorization checking {@link Grant#isAuthorized()}
     */
    public static Authorization requireAuthorized() {
        return builder().requireAuthorized().build();
    }

    /**
     * Requires the grant's clearance level to be at least {@code level}.
     *
     * @param level the minimum required clearance level
     * @return an authorization checking {@link Grant#level()}
     */
    public static Authorization requireClearance(int level) {
        return builder().requireClearance(level).build();
    }

    /**
     * Requires all given scopes to be present in the grant.
     *
     * @param scopes the required scopes
     * @return an authorization checking {@link Grant#hasScopes(String...)}
     */
    public static Authorization requireScopes(String... scopes) {
        return builder().requireScopes(scopes).build();
    }

    /**
     * Checks whether the given grant satisfies this authorization.
     * Returns {@code false} if the grant is {@code null}.
     *
     * @param grant the grant to check
     * @return {@code true} if the grant meets the requirements
     */
    public boolean check(Grant grant) {
        if (grant == null) return false;
        return checker.test(grant);
    }

    /**
     * Builder for combining multiple authorization requirements using logical AND.
     */
    public static final class Builder {
        private Predicate<Grant> checker = Objects::nonNull;

        private Builder() {}

        /**
         * Adds a requirement that the grant must be authorized.
         *
         * @return this builder
         */
        public Builder requireAuthorized() {
            this.checker = this.checker.and(Grant::isAuthorized);
            return this;
        }

        /**
         * Adds a requirement that the grant clearance level must be at least the given level.
         *
         * @param level the minimum clearance level
         * @return this builder
         */
        public Builder requireClearance(int level) {
            this.checker = this.checker.and(grant -> grant.level() >= level);
            return this;
        }

        /**
         * Adds a requirement that the grant must contain all the given scopes.
         *
         * @param scopes the required scopes
         * @return this builder
         */
        public Builder requireScopes(String... scopes) {
            if (scopes != null && scopes.length > 0) {
                Set<String> required = Set.of(scopes);
                this.checker = this.checker.and(grant -> grant.hasScopes(required));
            }
            return this;
        }

        /**
         * Builds the composite {@link Authorization} instance.
         *
         * @return the constructed authorization
         */
        public Authorization build() {
            return new Authorization(this.checker);
        }
    }
}