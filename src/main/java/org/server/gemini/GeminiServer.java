package org.server.gemini;

import org.server.gemini.internal.CertificateManager;
import org.server.gemini.internal.RouteRegistry;
import org.server.gemini.internal.RouteScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * The main entry point for a Gemini protocol server. Scans the application for
 * {@link GeminiController @GeminiController} classes, builds a route registry,
 * and starts a Reactor Netty server with TLS on the specified port.
 *
 * <p>By default, the server generates a self-signed certificate on first startup
 * and persists it to {@code ./gemini-certs/} for reuse. This is the standard
 * approach for Gemini servers — clients use TOFU (Trust on First Use) to pin
 * the certificate, so a self-signed cert is valid for production use.
 *
 * <p>Zero-config startup:
 * <pre>{@code
 * GeminiServer.start(MyApp.class);
 * }</pre>
 *
 * <p>Custom configuration:
 * <pre>{@code
 * GeminiServer.start(MyApp.class, GeminiConfig.builder()
 *     .hostname("example.com")
 *     .port(1965)
 *     .build());
 * }</pre>
 *
 * <p>The server scans the package of the provided class (and all sub-packages)
 * for controllers, mirroring the convention used by Spring Boot's
 * {@code @SpringBootApplication}.
 */
public final class GeminiServer {

    private static final Logger log = LoggerFactory.getLogger(GeminiServer.class);

    private final RouteRegistry routeRegistry;
    private final CertificateManager certificateManager;
    private final GeminiConfig config;

    private GeminiServer(RouteRegistry routeRegistry, CertificateManager certificateManager, GeminiConfig config) {
        this.routeRegistry = routeRegistry;
        this.certificateManager = certificateManager;
        this.config = config;
    }

    /**
     * Starts the Gemini server with all default settings: port 1965, hostname
     * {@code localhost}, and auto-generated self-signed certificate.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     */
    public static void start(Class<?> applicationClass) {
        start(applicationClass, GeminiConfig.defaults());
    }

    /**
     * Starts the Gemini server with the given configuration.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @param config the server configuration
     */
    public static void start(Class<?> applicationClass, GeminiConfig config) {
        String basePackage = applicationClass.getPackageName();
        log.info("Scanning '{}' for @GeminiController classes", basePackage);

        RouteRegistry registry = RouteScanner.scan(basePackage);

        CertificateManager certManager;
        if (config.certPath() != null) {
            certManager = CertificateManager.load(config.certPath(), config.keyPath());
        } else {
            certManager = CertificateManager.loadOrGenerate(Path.of("gemini-certs"));
        }

        GeminiServer server = new GeminiServer(registry, certManager, config);

        log.info("Gemini server starting on {}:{}", config.hostname(), config.port());
        // TODO: start Reactor Netty with certManager.sslContext()
    }

    RouteRegistry routeRegistry() {
        return routeRegistry;
    }

    CertificateManager certificateManager() {
        return certificateManager;
    }

    GeminiConfig config() {
        return config;
    }
}
