package io.github.wallawood.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a default value for a parameter annotated with {@link PathParam}
 * or {@link QueryParam} when the value is absent from the request.
 *
 * <p>Equivalent to {@link jakarta.ws.rs.DefaultValue} — both are accepted.
 *
 * <pre>{@code
 * @Path("/search")
 * public GeminiResponse search(@QueryParam("q") @DefaultValue("*") String query) { ... }
 * }</pre>
 *
 * @see QueryParam
 * @see PathParam
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {
    /**
     * The default value.
     *
     * @return the default value string
     */
    String value();
}
