package io.gemboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Configuration for a Gemini server. Provides sensible defaults aligned with the
 * Gemini protocol specification, with options for programmatic or file-based
 * configuration.
 *
 * <p>Zero-config defaults:
 * <pre>{@code
 * GembootConfig config = GembootConfig.defaults();
 * // hostname: "localhost", port: 1965, auto-generated certificate
 * }</pre>
 *
 * <p>Programmatic configuration via builder:
 * <pre>{@code
 * GembootConfig config = GembootConfig.builder()
 *     .hostname("example.com")
 *     .port(1965)
 *     .certificate(Path.of("cert.pem"), Path.of("key.pem"))
 *     .staticDirectories(List.of("classpath:/public", "file:./content"))
 *     .build();
 * }</pre>
 *
 * <p>File-based configuration via properties:
 * <pre>{@code
 * GembootConfig config = GembootConfig.fromProperties(Path.of("application.properties"));
 * }</pre>
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code gemboot.hostname} — server hostname for TLS SNI and certificate CN (default: {@code localhost})</li>
 *   <li>{@code gemboot.bind-address} — address to bind the server socket to (default: {@code 0.0.0.0})</li>
 *   <li>{@code gemboot.port} — listening port (default: {@code 1965})</li>
 *   <li>{@code gemboot.cert.path} — path to certificate PEM file (optional)</li>
 *   <li>{@code gemboot.key.path} — path to private key PEM file (optional, required if cert.path is set)</li>
 *   <li>{@code gemboot.static.directories} — comma-separated list of additional static resource directories
 *       using {@code classpath:} or {@code file:} prefixes (e.g. {@code classpath:/public,file:./content}).
 *       The built-in {@code classpath:/static} is always resolved first.</li>
 * </ul>
 */
public final class GembootConfig {
    private static final Logger log = LoggerFactory.getLogger(GembootConfig.class);

    private static final String DEFAULT_HOSTNAME = "localhost";
    private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_PORT = 1965;

    private final String hostname;
    private final String bindAddress;
    private final int port;
    private final Path certPath;
    private final Path keyPath;
    private final List<String> staticDirectories;

    private GembootConfig(String hostname, String bindAddress, int port, Path certPath, Path keyPath,
                          List<String> staticDirectories) {
        this.hostname = hostname;
        this.bindAddress = bindAddress;
        this.port = port;
        this.certPath = certPath;
        this.keyPath = keyPath;
        this.staticDirectories = Collections.unmodifiableList(staticDirectories);
    }

    /**
     * Returns a configuration with all defaults: hostname {@code localhost},
     * port {@code 1965}, and auto-generated self-signed certificate.
     *
     * @return the default configuration
     */
    public static GembootConfig defaults() {
        return new GembootConfig(DEFAULT_HOSTNAME, DEFAULT_BIND_ADDRESS, DEFAULT_PORT, null, null, List.of());
    }

    /**
     * Returns a new builder for programmatic configuration.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads configuration from {@code application.properties} in the working directory.
     *
     * @return a configuration loaded from {@code application.properties}
     * @throws IllegalArgumentException if the file cannot be read or cert/key paths are inconsistent
     * @see #fromProperties(Path)
     */
    public static GembootConfig fromProperties() {
        return fromProperties(Path.of("application.properties"));
    }

    /**
     * Loads configuration from a properties file. Unspecified properties
     * fall back to defaults. If {@code gemboot.cert.path} is set,
     * {@code gemboot.key.path} must also be set.
     *
     * @param path path to the properties file
     * @return a configuration loaded from the file
     * @throws IllegalArgumentException if the file cannot be read or cert/key paths are inconsistent
     */
    public static GembootConfig fromProperties(Path path) {
        log.info("Loading properties from path {}", path);
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read properties file: " + path.toAbsolutePath(), e);
        }

        String hostname = props.getProperty("gemboot.hostname", DEFAULT_HOSTNAME);
        String bindAddress = props.getProperty("gemboot.bind-address", DEFAULT_BIND_ADDRESS);
        int port = Integer.parseInt(props.getProperty("gemboot.port", String.valueOf(DEFAULT_PORT)));
        String certStr = props.getProperty("gemboot.cert.path");
        String keyStr = props.getProperty("gemboot.key.path");

        Path certPath = certStr != null ? Path.of(certStr) : null;
        Path keyPath = keyStr != null ? Path.of(keyStr) : null;

        if ((certPath == null) != (keyPath == null)) {
            throw new IllegalArgumentException("Both gemboot.cert.path and gemboot.key.path must be set, or neither");
        }

        List<String> staticDirs = parseStaticDirectories(props.getProperty("gemboot.static.directories"));

        return new GembootConfig(hostname, bindAddress, port, certPath, keyPath, staticDirs);
    }

    private static List<String> parseStaticDirectories(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** Server hostname for TLS SNI and certificate CN.
     * @return the hostname */
    public String hostname() {
        return hostname;
    }

    /** Address to bind the server socket to. Defaults to {@code 0.0.0.0} (all interfaces).
     * @return the bind address */
    public String bindAddress() {
        return bindAddress;
    }

    /** Listening port. Gemini default is 1965.
     * @return the port number */
    public int port() {
        return port;
    }

    /** Path to the certificate PEM file, or {@code null} for auto-generation.
     * @return the certificate path, or {@code null} */
    public Path certPath() {
        return certPath;
    }

    /** Path to the private key PEM file, or {@code null} for auto-generation.
     * @return the key path, or {@code null} */
    public Path keyPath() {
        return keyPath;
    }

    /**
     * Additional static resource directories to search after the built-in {@code classpath:/static}.
     * Each entry uses a {@code classpath:} or {@code file:} prefix.
     *
     * @return an unmodifiable list of static directory locations
     */
    public List<String> staticDirectories() {
        return staticDirectories;
    }

    /** Builder for constructing a {@link GembootConfig} with custom values. */
    public static final class Builder {

        private String hostname = DEFAULT_HOSTNAME;
        private String bindAddress = DEFAULT_BIND_ADDRESS;
        private int port = DEFAULT_PORT;
        private Path certPath;
        private Path keyPath;
        private List<String> staticDirectories = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the server hostname, used for TLS SNI and as the CN in
         * auto-generated certificates.
         *
         * @param hostname the hostname (default: {@code localhost})
         * @return this builder
         */
        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        /**
         * Sets the address to bind the server socket to.
         *
         * @param bindAddress the bind address (default: {@code 0.0.0.0})
         * @return this builder
         */
        public Builder bindAddress(String bindAddress) {
            this.bindAddress = bindAddress;
            return this;
        }

        /**
         * Sets the listening port.
         *
         * @param port the port (default: {@code 1965})
         * @return this builder
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the paths to user-provided PEM certificate and key files.
         * When set, auto-generation is disabled and the server will fail fast
         * if the files do not exist.
         *
         * @param certPath path to the certificate PEM file
         * @param keyPath path to the private key PEM file
         * @return this builder
         */
        public Builder certificate(Path certPath, Path keyPath) {
            this.certPath = certPath;
            this.keyPath = keyPath;
            return this;
        }

        /**
         * Sets additional static resource directories to search after the built-in
         * {@code classpath:/static}. Each entry should use a {@code classpath:} or
         * {@code file:} prefix (e.g. {@code classpath:/public}, {@code file:./content}).
         *
         * @param staticDirectories the ordered list of additional static directories
         * @return this builder
         */
        public Builder staticDirectories(List<String> staticDirectories) {
            this.staticDirectories = new ArrayList<>(staticDirectories);
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return an immutable {@link GembootConfig}
         * @throws IllegalArgumentException if only one of cert/key path is set
         */
        public GembootConfig build() {
            if ((certPath == null) != (keyPath == null)) {
                throw new IllegalArgumentException("Both certPath and keyPath must be set, or neither");
            }
            return new GembootConfig(hostname, bindAddress, port, certPath, keyPath, staticDirectories);
        }
    }
}
