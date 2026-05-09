package io.github.wallawood.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects request context into a handler method parameter. Built-in types:
 *
 * <ul>
 *   <li>{@link java.net.URI} — the full request URI</li>
 *   <li>{@link java.security.cert.X509Certificate} — the client's TLS certificate
 *       (null if no certificate was presented)</li>
 * </ul>
 *
 * <p>Equivalent to {@link jakarta.ws.rs.core.Context} — both are accepted.
 *
 * <p><strong>Thread safety:</strong> Instances of this class are singletons shared across
 * Reactor Netty's event loop threads. Implementations must be thread-safe — avoid mutable
 * instance fields, or protect them with appropriate synchronization.
 *
 * <pre>{@code
 * @Path("/whoami")
 * public GeminiResponse whoami(@Context URI uri, @Context X509Certificate cert) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Context {
}
