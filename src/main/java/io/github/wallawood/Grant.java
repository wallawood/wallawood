package io.github.wallawood;

import io.github.wallawood.annotations.RequireAuthorized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An immutable authorization grant representing what a client has been given.
 * Attached to a {@link RequestContext} by a {@link RequestInterceptor @Preprocessor}
 * interceptor, typically after inspecting the client's TLS certificate.
 *
 * <p>A Grant describes <b>what the user has</b>. To describe <b>what is required</b>,
 * use {@link Authorization} or the security annotations
 * ({@link RequireAuthorized @RequireAuthorized},
 * {@link io.github.wallawood.annotations.RequireClearance @RequireClearance},
 * {@link io.github.wallawood.annotations.RequireScopes @RequireScopes}).
 *
 * <p>Three orthogonal dimensions, each checked independently:
 * <ul>
 * <li><b>Authorized</b> — a boolean flag checked by {@code @RequireAuthorized}</li>
 * <li><b>Clearance</b> — a numeric level checked by {@code @RequireClearance}</li>
 * <li><b>Scopes</b> — a set of permission strings checked by {@code @RequireScopes}</li>
 * </ul>
 *
 * <p>These dimensions do not imply each other. A grant with clearance 5 does not
 * automatically satisfy {@code @RequireAuthorized}, and vice versa. The application
 * decides which dimensions to set via the builder or factory methods.
 *
 * <p>Instances are immutable and safe to share across threads. Use {@link #builder()}
 * to construct grants spanning multiple dimensions.
 *
 * <pre>{@code
 * // In a @Preprocessor — set what the user has:
 * context.add(Grant.clearance(3));
 *
 * // Or combine dimensions:
 * context.add(Grant.builder()
 *     .authorized(true)
 *     .clearance(2)
 *     .addScope("read:messages")
 *     .build());
 *
 * // The framework checks annotations automatically:
 * @RequireClearance(level = 2)
 * @RequireScopes(scopes = "read:messages")
 * public GeminiResponse inbox() { ... }
 *
 * // Or check manually in a preprocessor:
 * Authorization.requireClearance(2).check(grant)
 * }</pre>
 *
 * @see Authorization
 * @see RequireAuthorized
 * @see io.github.wallawood.annotations.RequireClearance
 * @see io.github.wallawood.annotations.RequireScopes
 */
public final class Grant {

    private final boolean authorized;
    private final int level;
    private final Set<String> scopes;

    private Grant(boolean authorized, int level, Set<String> scopes) {
        this.authorized = authorized;
        this.level = level;
        this.scopes = Set.copyOf(scopes);
    }

    /**
     * Creates a new Builder for constructing a Grant.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * No permissions. Fails all authorization checks. Represents an
     * unauthenticated or explicitly denied client.
     *
     * @return a grant with no permissions
     */
    public static Grant none() {
        return new Grant(false, -1, Set.of());
    }

    /**
     * Simple authorized grant. Only satisfies {@code @RequireAuthorized} checks.
     * Does not imply any clearance level or scopes.
     *
     * @return an authorized-only grant
     */
    public static Grant authorized() {
        return builder().authorized(true).build();
    }

    /**
     * Level-based grant. Only satisfies {@code @RequireClearance} checks where
     * the required level is {@code <=} this level. Does not imply authorized
     * or any scopes.
     *
     * @param level the authorization level (higher = more access)
     * @return a clearance-only grant
     */
    public static Grant clearance(int level) {
        return builder().clearance(level).build();
    }

    /**
     * Scope-based grant. Only satisfies {@code @RequireScopes} checks where
     * all required scopes are present. Does not imply authorized or any
     * clearance level.
     *
     * @param scopes the granted scopes
     * @return a scopes-only grant
     */
    public static Grant scopes(String... scopes) {
        return builder().addScopes(scopes).build();
    }

    /**
     * Whether this grant satisfies {@code @RequireAuthorized} checks.
     *
     * @return {@code true} if authorized
     */
    public boolean isAuthorized() {
        return authorized;
    }

    /**
     * The clearance level, or {@code -1} if not set. Checked by {@code @RequireClearance}.
     *
     * @return the clearance level
     */
    public int level() {
        return level;
    }

    /**
     * The granted scopes (unmodifiable). Empty if not set. Checked by {@code @RequireScopes}.
     *
     * @return the scope set
     */
    public Set<String> scopes() {
        return scopes;
    }

    /**
     * Returns {@code true} if this grant contains all of the given scopes.
     *
     * @param required the scopes to check
     * @return {@code true} if all required scopes are present
     */
    public boolean hasScopes(String... required) {
        return scopes.containsAll(Arrays.asList(required));
    }

    /**
     * Returns {@code true} if this grant contains all of the given scopes.
     *
     * @param required the scopes to check
     * @return {@code true} if all required scopes are present
     */
    public boolean hasScopes(Set<String> required) {
        return scopes.containsAll(required);
    }

    /**
     * Builder for constructing immutable {@link Grant} instances.
     */
    public static final class Builder {
        private boolean authorized = false;
        private int level = -1;
        private final Set<String> scopes = new HashSet<>();

        private Builder() {
        }

        /**
         * Sets the simple authorization flag.
         *
         * @param authorized whether the grant is authorized
         * @return this builder
         */
        public Builder authorized(boolean authorized) {
            this.authorized = authorized;
            return this;
        }

        /**
         * Sets the authorization clearance level.
         *
         * @param clearance the clearance level
         * @return this builder
         */
        public Builder clearance(int clearance) {
            this.level = clearance;
            return this;
        }

        /**
         * Adds a single scope to this grant.
         *
         * @param scope the scope to add
         * @return this builder
         */
        public Builder addScope(String scope) {
            if (scope != null) {
                this.scopes.add(scope);
            }
            return this;
        }

        /**
         * Adds multiple scopes to this grant.
         *
         * @param scopes the scopes to add
         * @return this builder
         */
        public Builder addScopes(String... scopes) {
            if (scopes != null) {
                this.scopes.addAll(Arrays.asList(scopes));
            }
            return this;
        }

        /**
         * Adds a collection of scopes to this grant.
         *
         * @param scopes the scopes to add
         * @return this builder
         */
        public Builder addScopes(Set<String> scopes) {
            if (scopes != null) {
                this.scopes.addAll(scopes);
            }
            return this;
        }

        /**
         * Builds and returns the immutable {@link Grant} instance.
         *
         * @return the constructed grant
         */
        public Grant build() {
            return new Grant(authorized, level, scopes);
        }
    }
}
