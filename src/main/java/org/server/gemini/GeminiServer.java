package org.server.gemini;

import org.server.gemini.internal.RouteRegistry;
import org.server.gemini.internal.RouteScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry point for a Gemini protocol server. Scans the application for
 * {@link GeminiController @GeminiController} classes, builds a route registry,
 * and starts a Reactor Netty server with TLS on the specified port.
 *
 * <p>Usage:
 * <pre>{@code
 * public class MyApp {
 *     public static void main(String[] args) {
 *         GeminiServer.start(MyApp.class, 1965);
 *     }
 * }
 * }</pre>
 *
 * <p>The server scans the package of the provided class (and all sub-packages)
 * for controllers, mirroring the convention used by Spring Boot's
 * {@code @SpringBootApplication}.
 */
public final class GeminiServer {

    private static final Logger log = LoggerFactory.getLogger(GeminiServer.class);

    private final RouteRegistry routeRegistry;

    private GeminiServer(RouteRegistry routeRegistry) {
        this.routeRegistry = routeRegistry;
    }

    /**
     * Scans the package of the given class for {@link GeminiController @GeminiController}
     * classes and starts the Gemini server on the specified port.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @param port the port to listen on (Gemini default is 1965)
     */
    public static void start(Class<?> applicationClass, int port) {
        String basePackage = applicationClass.getPackageName();
        log.info("Scanning '{}' for @GeminiController classes", basePackage);

        RouteRegistry registry = RouteScanner.scan(basePackage);
        GeminiServer server = new GeminiServer(registry);

        log.info("Gemini server starting on port {}", port);
        // TODO: start Reactor Netty with TLS
    }

    RouteRegistry routeRegistry() {
        return routeRegistry;
    }
}
