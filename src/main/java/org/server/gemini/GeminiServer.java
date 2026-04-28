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
 * <p>Default usage (auto-generated certificate):
 * <pre>{@code
 * public class MyApp {
 *     public static void main(String[] args) {
 *         GeminiServer.start(MyApp.class, 1965);
 *     }
 * }
 * }</pre>
 *
 * <p>With user-provided PEM files (e.g. migrating from another server or using
 * a CA-signed certificate):
 * <pre>{@code
 * public class MyApp {
 *     public static void main(String[] args) {
 *         GeminiServer.start(MyApp.class, 1965,
 *             Path.of("/etc/gemini/cert.pem"),
 *             Path.of("/etc/gemini/key.pem"));
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
    private static final Path DEFAULT_CERT_DIR = Path.of("gemini-certs");

    private final RouteRegistry routeRegistry;
    private final CertificateManager certificateManager;

    private GeminiServer(RouteRegistry routeRegistry, CertificateManager certificateManager) {
        this.routeRegistry = routeRegistry;
        this.certificateManager = certificateManager;
    }

    /**
     * Scans the package of the given class for {@link GeminiController @GeminiController}
     * classes and starts the Gemini server. A self-signed certificate is generated on
     * first startup and persisted to {@code ./gemini-certs/} for reuse. Subsequent starts
     * reuse the existing certificate to maintain TOFU compatibility with clients.
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @param port the port to listen on (Gemini default is 1965)
     */
    public static void start(Class<?> applicationClass, int port) {
        CertificateManager certManager = CertificateManager.loadOrGenerate(DEFAULT_CERT_DIR);
        start(applicationClass, port, certManager);
    }

    /**
     * Scans the package of the given class for {@link GeminiController @GeminiController}
     * classes and starts the Gemini server with user-provided PEM certificate files.
     * Use this when migrating from another Gemini server or when a specific certificate
     * is required (e.g. CA-signed).
     *
     * @param applicationClass the anchor class whose package is used as the scan root
     * @param port the port to listen on (Gemini default is 1965)
     * @param certPath path to the certificate PEM file
     * @param keyPath path to the private key PEM file
     */
    public static void start(Class<?> applicationClass, int port, Path certPath, Path keyPath) {
        CertificateManager certManager = CertificateManager.load(certPath, keyPath);
        start(applicationClass, port, certManager);
    }

    private static void start(Class<?> applicationClass, int port, CertificateManager certManager) {
        String basePackage = applicationClass.getPackageName();
        log.info("Scanning '{}' for @GeminiController classes", basePackage);

        RouteRegistry registry = RouteScanner.scan(basePackage);
        GeminiServer server = new GeminiServer(registry, certManager);

        log.info("Gemini server starting on port {}", port);
        // TODO: start Reactor Netty with certManager.sslContext()
    }

    RouteRegistry routeRegistry() {
        return routeRegistry;
    }

    CertificateManager certificateManager() {
        return certificateManager;
    }
}
