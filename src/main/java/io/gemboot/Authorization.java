package io.gemboot;

import java.util.Set;

/**
 * An immutable authorization requirement that can be checked against a
 * {@link Grant}. Use this in {@link RequestInterceptor @Preprocessor}
 * interceptors to guard routes with the same logic the framework uses
 * for {@link io.gemboot.annotations.Authorize @Authorize}.
 *
 * <p>Three modes:
 * <ul>
 *   <li>{@link #level(int)} — requires {@code grant.level() >= level}</li>
 *   <li>{@link #scopes(String...)} — requires all scopes present in the grant</li>
 *   <li>Both via {@code &&} — requires level AND scopes</li>
 * </ul>
 *
 * <pre>{@code
 * // In a @Preprocessor:
 * private static final Authorization MOD = Authorization.level(3);
 *
 * if (!MOD.check(grant)) {
 *     return Optional.of(GeminiResponse.certificateNotAuthorized("Mods only."));
 * }
 *
 * // Combine with plain Java:
 * Authorization.level(2).check(grant) && Authorization.scopes("write").check(grant)
 * }</pre>
 *
 * @see Grant
 * @see io.gemboot.annotations.Authorize
 */
public final class Authorization {

    private final int requiredLevel;
    private final Set<String> requiredScopes;

    /**
     * Creates an authorization requiring the given level and scopes (AND semantics).
     * Use {@link #level(int)} or {@link #scopes(String...)} for single-mode checks.
     *
     * @param requiredLevel minimum level ({@code -1} to skip level check)
     * @param requiredScopes required scopes (empty to skip scope check)
     */
    public Authorization(int requiredLevel, String... requiredScopes) {
        this.requiredLevel = requiredLevel;
        this.requiredScopes = requiredScopes != null && requiredScopes.length > 0
                ? Set.of(requiredScopes) : Set.of();
    }

    /**
     * Requires the grant's level to be at least {@code level}.
     *
     * @param level the minimum required level
     * @return an authorization that checks level
     */
    public static Authorization level(int level) {
        return new Authorization(level);
    }

    /**
     * Requires all given scopes to be present in the grant.
     *
     * @param scopes the required scopes
     * @return an authorization that checks scopes
     */
    public static Authorization scopes(String... scopes) {
        return new Authorization(-1, scopes);
    }

    /**
     * Checks whether the given grant satisfies this authorization.
     * Returns {@code false} if the grant is {@code null} or not authorized.
     *
     * @param grant the grant to check
     * @return {@code true} if the grant meets the requirements
     */
    public boolean check(Grant grant) {
        if (grant == null || !grant.isAuthorized()) return false;
        if (requiredLevel >= 0 && grant.level() < requiredLevel) return false;
        if (!requiredScopes.isEmpty() && !grant.hasScopes(requiredScopes)) return false;
        return true;
    }
}
