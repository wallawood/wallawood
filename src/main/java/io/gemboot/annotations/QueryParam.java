package io.gemboot.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to a named query parameter from the request URI.
 * Query parameters are parsed from the {@code ?key=value&key2=value2} portion
 * of the Gemini URL.
 *
 * <p>For Gemini's single-value query string (the raw text after {@code ?}),
 * use {@link QueryString} instead.
 *
 * <p>Equivalent to {@link jakarta.ws.rs.QueryParam} — both are accepted.
 *
 * <pre>{@code
 * @Path("/greet")
 * public GeminiResponse greet(@QueryParam("name") @DefaultValue("world") String name) { ... }
 * }</pre>
 *
 * @see DefaultValue
 * @see QueryString
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParam {
    /** The name of the query parameter to bind. */
    String value();
}
