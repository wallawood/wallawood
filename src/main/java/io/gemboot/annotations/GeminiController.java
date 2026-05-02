package io.gemboot.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Gemini request handler. Classes annotated with {@code @GeminiController}
 * are discovered during classpath scanning and their methods annotated with
 * {@link jakarta.ws.rs.Path @Path} are registered as Gemini routes.
 *
 * <p>An optional class-level {@link jakarta.ws.rs.Path @Path} may be used to define a
 * common prefix for all routes in the controller.
 *
 * <p><strong>Thread safety:</strong> Instances of this class are singletons shared across
 * Reactor Netty's event loop threads. Implementations must be thread-safe — avoid mutable
 * instance fields, or protect them with appropriate synchronization.
 *
 * <pre>{@code
 * @GeminiController
 * @Path("/users")
 * public class UserController {
 *
 *     @Path("/{id}")
 *     public GeminiResponse getUser(@PathParam("id") String id) {
 *         return GeminiResponse.success("# User " + id);
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GeminiController {
}
