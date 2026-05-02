package io.gemboot;

import io.gemboot.annotations.GeminiController;
import io.gemboot.internal.CertificateManager;
import io.gemboot.internal.GembootServerEngine;
import io.gemboot.internal.ScanResult;
import io.gemboot.internal.RouteScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.DisposableServer;

import java.nio.file.Files;
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
 * GembootServer.start(MyApp.class);
 * }</pre>
 *
 * <p>Non-blocking startup with graceful shutdown:
 * <pre>{@code
 * GembootServer server = GembootServer.launch(MyApp.class);
 * // ... later ...
 * server.stop();
 * }</pre>
 *
 * <p>The server scans the package of the provided class (and all sub-packages)
 * for controllers, mirroring the convention used by Spring Boot's
 * {@code @SpringBootApplication}.
 */
public final class GembootServer {

    private static final Logger log = LoggerFactory.getLogger(GembootServer.class);

    private final DisposableServer server;
    private final GembootConfig config;

    private static final Path DEFAULT_PROPERTIES = Path.of("application.properties");

    private GembootServer(DisposableServer server, GembootConfig config) {
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
        start(applicationClass, resolveConfig());
    }

    /**
     * Starts the Gemini server with the given configuration and blocks until shutdown.
     * Registers a JVM shutdown hook for graceful termination on SIGTERM/SIGINT.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @param config the server configuration
     */
    public static void start(Class<?> applicationClass, GembootConfig config) {
        GembootServer server = launch(applicationClass, config);
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
    public static GembootServer launch(Class<?> applicationClass) {
        return launch(applicationClass, resolveConfig());
    }

    /**
     * Starts the Gemini server with the given configuration without blocking.
     * The caller is responsible for calling {@link #stop()} when done.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @param config the server configuration
     * @return a running server instance
     */
    public static GembootServer launch(Class<?> applicationClass, GembootConfig config) {
        String basePackage = applicationClass.getPackageName();
        ScanResult scanResult = RouteScanner.scan(basePackage);

        CertificateManager certManager;
        if (config.certPath() != null) {
            certManager = CertificateManager.load(config.certPath(), config.keyPath());
        } else {
            certManager = CertificateManager.loadOrGenerate(Path.of("gemini-certs"), config.hostname());
        }

        GembootServerEngine engine = new GembootServerEngine(
                scanResult.routeRegistry(), certManager, scanResult.exceptionResolver(),
                scanResult.interceptors(), config);
        DisposableServer server = engine.startNonBlocking();

        return new GembootServer(server, config);
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
     *
     * @return the bound port
     */
    public int port() {
        return server.port();
    }

    /**
     * Returns the server configuration.
     *
     * @return the config
     */
    public GembootConfig config() {
        return config;
    }

    /**
     * Loads {@code application.properties} from the working directory if it exists,
     * otherwise falls back to {@link GembootConfig#defaults()}. If the file exists
     * but cannot be parsed, the error propagates — a broken config file will
     * never silently fall back to defaults.
     */
    private static GembootConfig resolveConfig() {
        if (Files.exists(DEFAULT_PROPERTIES)) {
            return GembootConfig.fromProperties();
        }
        return GembootConfig.defaults();
    }
}
