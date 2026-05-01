package io.gemboot.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to a named path variable extracted from the
 * request URI. The variable name must match a {@code {name}} segment in
 * the {@link Path} pattern.
 *
 * <p>Supports {@code String}, {@code int}/{@code Integer}, and
 * {@code long}/{@code Long} parameter types.
 *
 * <p>Equivalent to {@link jakarta.ws.rs.PathParam} — both are accepted.
 *
 * <pre>{@code
 * @Path("/users/{id}")
 * public GeminiResponse show(@PathParam("id") int id) { ... }
 * }</pre>
 *
 * @see Path
 * @see DefaultValue
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathParam {
    /** The name of the path variable to bind. */
    String value();
}
