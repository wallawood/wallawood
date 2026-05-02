package io.gemboot.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires authorization before invoking the handler. Place on a method or
 * class. Class-level applies to all handlers in the controller.
 *
 * <p>Authorization is checked against the {@link io.gemboot.Grant Grant} in the
 * {@link io.gemboot.RequestContext RequestContext}, which is typically set by a
 * {@link Preprocessor @Preprocessor} interceptor. If no client certificate is
 * present, the server responds with status 60 (Client Certificate Required).
 *
 * <p>Three modes, determined by which attributes are set:
 * <ul>
 *   <li><b>Simple</b> — {@code @Authorize} — checks {@code grant.isAuthorized()}</li>
 *   <li><b>Level</b> — {@code @Authorize(level = 3)} — checks {@code grant.level() >= 3}</li>
 *   <li><b>Scopes</b> — {@code @Authorize(scopes = "notes:write")} — checks
 *       {@code grant.hasScopes("notes:write")}</li>
 *   <li><b>Both</b> — {@code @Authorize(level = 2, scopes = "write")} — AND semantics,
 *       both level and scopes must be satisfied</li>
 * </ul>
 *
 * <p>If no {@code Grant} is present in the context, the check fails.
 *
 * <p>Under the hood, the framework builds an {@link io.gemboot.Authorization Authorization}
 * from this annotation's attributes. To perform the same checks in a
 * {@link Preprocessor @Preprocessor}, use {@link io.gemboot.Authorization} directly:
 *
 * <pre>{@code
 * private static final Authorization MOD = Authorization.level(3);
 *
 * if (!MOD.check(grant)) {
 *     return Optional.of(GeminiResponse.certificateNotAuthorized("Mods only."));
 * }
 * }</pre>
 *
 * @see io.gemboot.Grant
 * @see io.gemboot.Authorization
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Authorize {

    /** Required scopes. If non-empty, all must be present in the grant. */
    String[] scopes() default {};

    /** Required level. If {@code >= 0}, the grant's level must be {@code >=} this value. */
    int level() default -1;

    /** Response message when authorization fails. Defaults to {@code "Not authorized"}. */
    String message() default "Not authorized";
}
