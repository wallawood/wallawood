package org.server.gemini;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration for a Gemini server. Provides sensible defaults aligned with the
 * Gemini protocol specification, with options for programmatic or file-based
 * configuration.
 *
 * <p>Zero-config defaults:
 * <pre>{@code
 * GeminiConfig config = GeminiConfig.defaults();
 * // hostname: "localhost", port: 1965, auto-generated certificate
 * }</pre>
 *
 * <p>Programmatic configuration via builder:
 * <pre>{@code
 * GeminiConfig config = GeminiConfig.builder()
 *     .hostname("example.com")
 *     .port(1965)
 *     .certificate(Path.of("cert.pem"), Path.of("key.pem"))
 *     .build();
 * }</pre>
 *
 * <p>File-based configuration via properties:
 * <pre>{@code
 * GeminiConfig config = GeminiConfig.fromProperties(Path.of("gemini.properties"));
 * }</pre>
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code gemini.hostname} — server hostname for TLS SNI and certificate CN (default: {@code localhost})</li>
 *   <li>{@code gemini.bind-address} — address to bind the server socket to (default: {@code 0.0.0.0})</li>
 *   <li>{@code gemini.port} — listening port (default: {@code 1965})</li>
 *   <li>{@code gemini.cert.path} — path to certificate PEM file (optional)</li>
 *   <li>{@code gemini.key.path} — path to private key PEM file (optional, required if cert.path is set)</li>
 * </ul>
 */
public final class GeminiConfig {

    private static final String DEFAULT_HOSTNAME = "localhost";
    private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_PORT = 1965;

    private final String hostname;
    private final String bindAddress;
    private final int port;
    private final Path certPath;
    private final Path keyPath;

    private GeminiConfig(String hostname, String bindAddress, int port, Path certPath, Path keyPath) {
        this.hostname = hostname;
        this.bindAddress = bindAddress;
        this.port = port;
        this.certPath = certPath;
        this.keyPath = keyPath;
    }

    /**
     * Returns a configuration with all defaults: hostname {@code localhost},
     * port {@code 1965}, and auto-generated self-signed certificate.
     *
     * @return the default configuration
     */
    public static GeminiConfig defaults() {
        return new GeminiConfig(DEFAULT_HOSTNAME, DEFAULT_BIND_ADDRESS, DEFAULT_PORT, null, null);
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
     * Loads configuration from {@code gemini.properties} in the working directory.
     *
     * @return a configuration loaded from {@code gemini.properties}
     * @throws IllegalArgumentException if the file cannot be read or cert/key paths are inconsistent
     * @see #fromProperties(Path)
     */
    public static GeminiConfig fromProperties() {
        return fromProperties(Path.of("gemini.properties"));
    }

    /**
     * Loads configuration from a properties file. Unspecified properties
     * fall back to defaults. If {@code gemini.cert.path} is set,
     * {@code gemini.key.path} must also be set.
     *
     * @param path path to the properties file
     * @return a configuration loaded from the file
     * @throws IllegalArgumentException if the file cannot be read or cert/key paths are inconsistent
     */
    public static GeminiConfig fromProperties(Path path) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read properties file: " + path, e);
        }

        String hostname = props.getProperty("gemini.hostname", DEFAULT_HOSTNAME);
        String bindAddress = props.getProperty("gemini.bind-address", DEFAULT_BIND_ADDRESS);
        int port = Integer.parseInt(props.getProperty("gemini.port", String.valueOf(DEFAULT_PORT)));
        String certStr = props.getProperty("gemini.cert.path");
        String keyStr = props.getProperty("gemini.key.path");

        Path certPath = certStr != null ? Path.of(certStr) : null;
        Path keyPath = keyStr != null ? Path.of(keyStr) : null;

        if ((certPath == null) != (keyPath == null)) {
            throw new IllegalArgumentException("Both gemini.cert.path and gemini.key.path must be set, or neither");
        }

        return new GeminiConfig(hostname, bindAddress, port, certPath, keyPath);
    }

    /** Server hostname for TLS SNI and certificate CN. */
    public String hostname() {
        return hostname;
    }

    /** Address to bind the server socket to. Defaults to {@code 0.0.0.0} (all interfaces). */
    public String bindAddress() {
        return bindAddress;
    }

    /** Listening port. Gemini default is 1965. */
    public int port() {
        return port;
    }

    /** Path to the certificate PEM file, or {@code null} for auto-generation. */
    public Path certPath() {
        return certPath;
    }

    /** Path to the private key PEM file, or {@code null} for auto-generation. */
    public Path keyPath() {
        return keyPath;
    }

    public static final class Builder {

        private String hostname = DEFAULT_HOSTNAME;
        private String bindAddress = DEFAULT_BIND_ADDRESS;
        private int port = DEFAULT_PORT;
        private Path certPath;
        private Path keyPath;

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
         * Builds the configuration.
         *
         * @return an immutable {@link GeminiConfig}
         * @throws IllegalArgumentException if only one of cert/key path is set
         */
        public GeminiConfig build() {
            if ((certPath == null) != (keyPath == null)) {
                throw new IllegalArgumentException("Both certPath and keyPath must be set, or neither");
            }
            return new GeminiConfig(hostname, bindAddress, port, certPath, keyPath);
        }
    }
}
