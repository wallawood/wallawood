package io.gemboot.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a controller class or handler method to a URI path pattern.
 *
 * <p>On a class, defines a route prefix applied to all methods in the controller.
 * On a method, defines the route that triggers the handler. Path variables use
 * curly-brace syntax (e.g. {@code "/users/{id}"}) and are injected via
 * {@link PathParam}.
 *
 * <p>Equivalent to {@link jakarta.ws.rs.Path} — both are accepted by the
 * route scanner.
 *
 * <pre>{@code
 * @GeminiController
 * @Path("/users")
 * public class UserController {
 *
 *     @Path("/{id}")
 *     public GeminiResponse show(@PathParam("id") int id) { ... }
 * }
 * }</pre>
 *
 * @see PathParam
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Path {
    /** The URI path pattern. */
    String value();
}
