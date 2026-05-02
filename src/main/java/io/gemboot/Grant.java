package io.gemboot;

import java.util.Set;

/**
 * An immutable authorization grant representing what a client is allowed to do.
 * Attached to a {@link RequestContext} by a {@link RequestInterceptor @Preprocessor}
 * interceptor, typically after inspecting the client's TLS certificate.
 *
 * <p>A Grant describes <b>what the user has</b>. To describe <b>what is required</b>,
 * use {@link Authorization} or the {@link io.gemboot.annotations.Authorize @Authorize}
 * annotation.
 *
 * <p>Supports three authorization models:
 * <ul>
 *   <li><b>Simple</b> — {@link #all()} or {@link #none()} for authenticated/unauthenticated</li>
 *   <li><b>Level-based</b> — {@link #at(int)} for role hierarchies (e.g. user=1, mod=2, admin=3)</li>
 *   <li><b>Scope-based</b> — {@link #some(String...)} for fine-grained permissions</li>
 * </ul>
 *
 * <p>Instances are immutable and safe to share across threads.
 *
 * <pre>{@code
 * // In a @Preprocessor — set what the user has:
 * context.add(Grant.at(3));
 *
 * // The framework checks it automatically via @Authorize:
 * @Authorize(level = 2)
 * public GeminiResponse adminPanel() { ... }
 *
 * // Or check manually in a preprocessor via Authorization:
 * Authorization.level(2).check(grant)
 * }</pre>
 *
 * @see Authorization
 * @see io.gemboot.annotations.Authorize
 */
public final class Grant {

    private final boolean authorized;
    private final boolean all;
    private final int level;
    private final Set<String> scopes;

    private Grant(boolean authorized, boolean all, int level, Set<String> scopes) {
        this.authorized = authorized;
        this.all = all;
        this.level = level;
        this.scopes = Set.copyOf(scopes);
    }

    /** No permissions. Authorization checks will fail. */
    public static Grant none() {
        return new Grant(false, false, -1, Set.of());
    }

    /**
     * Full permissions. Bypasses all authorization checks — level requirements,
     * scope requirements, and simple {@code isAuthorized()} checks will all pass.
     * Intended for superadmin or testing scenarios.
     */
    public static Grant all() {
        return new Grant(true, true, Integer.MAX_VALUE, Set.of());
    }

    /**
     * Level-based grant. Passes {@link io.gemboot.annotations.Authorize @Authorize}
     * checks where the required level is {@code <=} this level.
     *
     * @param level the authorization level (higher = more access)
     */
    public static Grant at(int level) {
        return new Grant(true, false, level, Set.of());
    }

    /**
     * Scope-based grant. Passes {@link io.gemboot.annotations.Authorize @Authorize}
     * checks where all required scopes are present in this grant.
     *
     * @param scopes the granted scopes
     */
    public static Grant some(String... scopes) {
        return new Grant(true, false, -1, Set.of(scopes));
    }

    /** Whether this grant represents an authenticated request. */
    public boolean isAuthorized() {
        return authorized;
    }

    /** The authorization level, or {@code -1} if not level-based. */
    public int level() {
        return level;
    }

    /** The granted scopes (unmodifiable). Empty if not scope-based. */
    public Set<String> scopes() {
        return scopes;
    }

    /** Returns {@code true} if this grant contains all of the given scopes. */
    public boolean hasScopes(String... required) {
        return all || scopes.containsAll(Set.of(required));
    }

    /** Returns {@code true} if this grant contains all of the given scopes. */
    public boolean hasScopes(Set<String> required) {
        return all || scopes.containsAll(required);
    }
}
