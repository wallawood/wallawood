package io.github.wallawood.annotations;

import io.github.wallawood.Authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the client's {@link io.github.wallawood.Grant Grant} to have a
 * {@link io.github.wallawood.Grant#level() clearance level} at least equal to the
 * specified value. Place on a method or class. Class-level applies to all
 * handlers in the controller.
 *
 * <p>If no client certificate is present, the server responds with status 60
 * (Client Certificate Required). If a certificate is present but the grant's
 * level is insufficient, the server responds with status 61 (Certificate Not Authorized).
 *
 * <p>This annotation checks only the clearance level. It does not check the
 * authorized flag or scopes — those are separate concerns handled by
 * {@link RequireAuthorized @RequireAuthorized} and {@link RequireScopes @RequireScopes}.
 * Stack multiple annotations on a method for AND semantics:
 *
 * <pre>{@code
 * @RequireClearance(level = 3)
 * @RequireScopes(scopes = "mod:ban")
 * public GeminiResponse banUser() { ... }
 * }</pre>
 *
 * <p>For complex OR logic or conditional checks, use a
 * {@link Preprocessor @Preprocessor} with {@link io.github.wallawood.Authorization} directly.
 *
 * @see io.github.wallawood.Grant#level()
 * @see io.github.wallawood.Authorization
 * @see RequireAuthorized
 * @see RequireScopes
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireClearance {

    /**
     * Minimum clearance level required. The grant's
     * {@link io.github.wallawood.Grant#level() level()} must be {@code >=} this value.
     *
     * @return the minimum level
     */
    int level();

    /**
     * Response message sent with the status 61 response when authorization fails.
     * Defaults to {@code "Access Denied"}.
     *
     * @return the failure message
     */
    String message() default Authorization.DEFAULT_MESSAGE;
}
