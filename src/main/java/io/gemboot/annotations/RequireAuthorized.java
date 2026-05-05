package io.gemboot.annotations;

import io.gemboot.Authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the client's {@link io.gemboot.Grant Grant} to have
 * {@link io.gemboot.Grant#isAuthorized() isAuthorized()} return {@code true}.
 * Place on a method or class. Class-level applies to all handlers in the controller.
 *
 * <p>If no client certificate is present, the server responds with status 60
 * (Client Certificate Required). If a certificate is present but the grant
 * fails the check, the server responds with status 61 (Certificate Not Authorized).
 *
 * <p>This annotation checks only the authorized flag. It does not check clearance
 * levels or scopes — those are separate concerns handled by
 * {@link RequireClearance @RequireClearance} and {@link RequireScopes @RequireScopes}.
 * Stack multiple annotations on a method for AND semantics:
 *
 * <pre>{@code
 * @RequireAuthorized
 * @RequireClearance(level = 3)
 * @RequireScopes(scopes = "admin:write")
 * public GeminiResponse adminPanel() { ... }
 * }</pre>
 *
 * <p>For complex OR logic or conditional checks, use a
 * {@link Preprocessor @Preprocessor} with {@link io.gemboot.Authorization} directly.
 *
 * @see io.gemboot.Grant#isAuthorized()
 * @see io.gemboot.Authorization
 * @see RequireClearance
 * @see RequireScopes
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireAuthorized {
    /**
     * Response message sent with the status 61 response when authorization fails.
     * Defaults to {@code "Access Denied"}.
     *
     * @return the failure message
     */
    String message() default Authorization.DEFAULT_MESSAGE;
}
