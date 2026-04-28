package org.server.gemini;

import org.server.gemini.internal.CertificateManager;
import org.server.gemini.internal.GeminiServerEngine;
import org.server.gemini.internal.ScanResult;
import org.server.gemini.internal.RouteScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.DisposableServer;

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
 * <p>Blocking startup (blocks the calling thread until shutdown):
 * <pre>{@code
 * GeminiServer.start(MyApp.class);
 * }</pre>
 *
 * <p>Non-blocking startup with graceful shutdown:
 * <pre>{@code
 * GeminiServer server = GeminiServer.launch(MyApp.class);
 * // ... later ...
 * server.stop();
 * }</pre>
 *
 * <p>The server scans the package of the provided class (and all sub-packages)
 * for controllers, mirroring the convention used by Spring Boot's
 * {@code @SpringBootApplication}.
 */
public final class GeminiServer {

    private static final Logger log = LoggerFactory.getLogger(GeminiServer.class);

    private final DisposableServer server;
    private final GeminiConfig config;

    private GeminiServer(DisposableServer server, GeminiConfig config) {
        this.server = server;
        this.config = config;
    }

    /**
     * Starts the Gemini server with default settings and blocks until shutdown.
     * Registers a JVM shutdown hook for graceful termination on SIGTERM/SIGINT.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     */
    public static void start(Class<?> applicationClass) {
        start(applicationClass, GeminiConfig.defaults());
    }

    /**
     * Starts the Gemini server with the given configuration and blocks until shutdown.
     * Registers a JVM shutdown hook for graceful termination on SIGTERM/SIGINT.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @param config the server configuration
     */
    public static void start(Class<?> applicationClass, GeminiConfig config) {
        GeminiServer server = launch(applicationClass, config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Gemini server...");
            server.stop();
        }));
        server.server.onDispose().block();
    }

    /**
     * Starts the Gemini server with default settings without blocking.
     * The caller is responsible for calling {@link #stop()} when done.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @return a running server instance
     */
    public static GeminiServer launch(Class<?> applicationClass) {
        return launch(applicationClass, GeminiConfig.defaults());
    }

    /**
     * Starts the Gemini server with the given configuration without blocking.
     * The caller is responsible for calling {@link #stop()} when done.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @param config the server configuration
     * @return a running server instance
     */
    public static GeminiServer launch(Class<?> applicationClass, GeminiConfig config) {
        String basePackage = applicationClass.getPackageName();
        log.info("Scanning '{}' for @GeminiController classes", basePackage);

        ScanResult scanResult = RouteScanner.scan(basePackage);

        CertificateManager certManager;
        if (config.certPath() != null) {
            certManager = CertificateManager.load(config.certPath(), config.keyPath());
        } else {
            certManager = CertificateManager.loadOrGenerate(Path.of("gemini-certs"), config.hostname());
        }

        GeminiServerEngine engine = new GeminiServerEngine(
                scanResult.routeRegistry(), certManager, scanResult.exceptionResolver(), config);
        DisposableServer server = engine.startNonBlocking();

        return new GeminiServer(server, config);
    }

    /**
     * Gracefully stops the server, closing all connections.
     */
    public void stop() {
        server.disposeNow();
        log.info("Gemini server stopped");
    }

    /**
     * Returns the port the server is listening on.
     */
    public int port() {
        return server.port();
    }

    /**
     * Returns the server configuration.
     */
    public GeminiConfig config() {
        return config;
    }
}
