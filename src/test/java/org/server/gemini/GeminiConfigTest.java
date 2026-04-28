package org.server.gemini;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GeminiConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsUsesLocalhostAnd1965() {
        GeminiConfig config = GeminiConfig.defaults();
        assertEquals("localhost", config.hostname());
        assertEquals(1965, config.port());
        assertNull(config.certPath());
        assertNull(config.keyPath());
    }

    @Test
    void builderWithAllOptions() {
        Path cert = Path.of("cert.pem");
        Path key = Path.of("key.pem");

        GeminiConfig config = GeminiConfig.builder()
                .hostname("example.com")
                .port(1966)
                .certificate(cert, key)
                .build();

        assertEquals("example.com", config.hostname());
        assertEquals(1966, config.port());
        assertEquals(cert, config.certPath());
        assertEquals(key, config.keyPath());
    }

    @Test
    void builderDefaultsMatchDefaults() {
        GeminiConfig fromBuilder = GeminiConfig.builder().build();
        GeminiConfig fromDefaults = GeminiConfig.defaults();

        assertEquals(fromDefaults.hostname(), fromBuilder.hostname());
        assertEquals(fromDefaults.port(), fromBuilder.port());
        assertEquals(fromDefaults.certPath(), fromBuilder.certPath());
        assertEquals(fromDefaults.keyPath(), fromBuilder.keyPath());
    }

    @Test
    void builderRejectsCertWithoutKey() {
        assertThrows(IllegalArgumentException.class, () ->
                GeminiConfig.builder()
                        .certificate(Path.of("cert.pem"), null)
                        .build());
    }

    @Test
    void builderRejectsKeyWithoutCert() {
        assertThrows(IllegalArgumentException.class, () ->
                GeminiConfig.builder()
                        .certificate(null, Path.of("key.pem"))
                        .build());
    }

    @Test
    void fromPropertiesWithAllValues() throws IOException {
        Path propsFile = tempDir.resolve("gemini.properties");
        Files.writeString(propsFile, """
                gemini.hostname=example.com
                gemini.port=1966
                gemini.cert.path=/etc/gemini/cert.pem
                gemini.key.path=/etc/gemini/key.pem
                """);

        GeminiConfig config = GeminiConfig.fromProperties(propsFile);

        assertEquals("example.com", config.hostname());
        assertEquals(1966, config.port());
        assertEquals(Path.of("/etc/gemini/cert.pem"), config.certPath());
        assertEquals(Path.of("/etc/gemini/key.pem"), config.keyPath());
    }

    @Test
    void fromPropertiesWithDefaults() throws IOException {
        Path propsFile = tempDir.resolve("gemini.properties");
        Files.writeString(propsFile, "");

        GeminiConfig config = GeminiConfig.fromProperties(propsFile);

        assertEquals("localhost", config.hostname());
        assertEquals(1965, config.port());
        assertNull(config.certPath());
        assertNull(config.keyPath());
    }

    @Test
    void fromPropertiesPartialOverride() throws IOException {
        Path propsFile = tempDir.resolve("gemini.properties");
        Files.writeString(propsFile, "gemini.hostname=myhost.com\n");

        GeminiConfig config = GeminiConfig.fromProperties(propsFile);

        assertEquals("myhost.com", config.hostname());
        assertEquals(1965, config.port());
    }

    @Test
    void fromPropertiesRejectsCertWithoutKey() throws IOException {
        Path propsFile = tempDir.resolve("gemini.properties");
        Files.writeString(propsFile, "gemini.cert.path=/cert.pem\n");

        assertThrows(IllegalArgumentException.class,
                () -> GeminiConfig.fromProperties(propsFile));
    }

    @Test
    void fromPropertiesRejectsKeyWithoutCert() throws IOException {
        Path propsFile = tempDir.resolve("gemini.properties");
        Files.writeString(propsFile, "gemini.key.path=/key.pem\n");

        assertThrows(IllegalArgumentException.class,
                () -> GeminiConfig.fromProperties(propsFile));
    }

    @Test
    void fromPropertiesRejectsMissingFile() {
        assertThrows(IllegalArgumentException.class,
                () -> GeminiConfig.fromProperties(tempDir.resolve("nope.properties")));
    }
}
