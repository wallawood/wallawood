package io.github.wallawood.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the entire query string of the request URI to a handler method parameter.
 * The value is percent-decoded before injection.
 *
 * <p>In the Gemini protocol, the query string carries user input from a prior
 * 1x (Input) response. The client URI-encodes the user's input and appends it
 * as the query component. Unlike HTTP's {@code key=value} convention, Gemini's
 * query string is typically a single unstructured value.
 *
 * <p>For example, given the request {@code gemini://example.com/search?hello%20world},
 * the parameter receives {@code "hello world"}.
 *
 * <pre>{@code
 * @Path("/search")
 * public GeminiResponse search(@QueryString String input) {
 *     return GeminiResponse.success("# Results for: " + input);
 * }
 * }</pre>
 *
 * @see jakarta.ws.rs.QueryParam for HTTP-style key-value query parameter extraction
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface QueryString {
}
