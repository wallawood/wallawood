package org.server.gemini.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a handler method requires sensitive user input (e.g. a password)
 * via the Gemini query string. If the request has no query string (or an empty one),
 * the server automatically responds with status 11 (Sensitive Input) using the
 * specified prompt. The client will prompt the user without echoing input to the
 * screen, then re-request the same URI with the input as the query component.
 *
 * <pre>{@code
 * @Path("/login")
 * @RequireSensitiveInput("Enter your password:")
 * public GeminiResponse login(@QueryString String password, @Context X509Certificate cert) {
 *     // password is guaranteed non-null and non-empty
 *     return GeminiResponse.success("# Welcome");
 * }
 * }</pre>
 *
 * @see RequireInput for non-sensitive input
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireSensitiveInput {
    /** The prompt text displayed to the user. */
    String value();
}
