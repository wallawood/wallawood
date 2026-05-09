package io.github.wallawood.annotations;

import io.github.wallawood.Authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the client's {@link io.github.wallawood.Grant Grant} to contain all of the
 * specified {@link io.github.wallawood.Grant#scopes() scopes}. Place on a method or class.
 * Class-level applies to all handlers in the controller.
 *
 * <p>If no client certificate is present, the server responds with status 60
 * (Client Certificate Required). If a certificate is present but the grant is
 * missing any required scope, the server responds with status 61
 * (Certificate Not Authorized).
 *
 * <p>This annotation checks only scopes. It does not check the authorized flag
 * or clearance level — those are separate concerns handled by
 * {@link RequireAuthorized @RequireAuthorized} and {@link RequireClearance @RequireClearance}.
 * Stack multiple annotations on a method for AND semantics:
 *
 * <pre>{@code
 * @RequireAuthorized
 * @RequireScopes(scopes = {"notes:read", "notes:write"})
 * public GeminiResponse editNote() { ... }
 * }</pre>
 *
 * <p>For complex OR logic or conditional checks, use a
 * {@link Preprocessor @Preprocessor} with {@link io.github.wallawood.Authorization} directly.
 *
 * @see io.github.wallawood.Grant#hasScopes(String...)
 * @see io.github.wallawood.Authorization
 * @see RequireAuthorized
 * @see RequireClearance
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireScopes {

    /**
     * Required scopes. All listed scopes must be present in the grant's
     * {@link io.github.wallawood.Grant#scopes() scope set} for the check to pass.
     *
     * @return the required scopes
     */
    String[] scopes();

    /**
     * Response message sent with the status 61 response when authorization fails.
     * Defaults to {@code "Access Denied"}.
     *
     * @return the failure message
     */
    String message() default Authorization.DEFAULT_MESSAGE;
}
