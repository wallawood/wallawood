package io.gemboot.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a handler method requires user input via the Gemini query string.
 * If the request has no query string (or an empty one), the server automatically
 * responds with status 10 (Input) using the specified prompt. The client will
 * re-request the same URI with the user's input as the query component.
 *
 * <p>This annotation eliminates the common pattern of checking for null/empty
 * query strings at the start of every input-driven handler.
 *
 * <pre>{@code
 * @Path("/search")
 * @RequireInput("Enter a search term:")
 * public GeminiResponse search(@QueryString String input) {
 *     // input is guaranteed non-null and non-empty
 *     return GeminiResponse.success("# Results for: " + input);
 * }
 * }</pre>
 *
 * @see RequireSensitiveInput for sensitive input (e.g. passwords)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireInput {
    /** The prompt text displayed to the user. */
    String value();
}
