package io.gemboot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GembootConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsUsesLocalhostAnd1965() {
        GembootConfig config = GembootConfig.defaults();
        assertEquals("localhost", config.hostname());
        assertEquals("0.0.0.0", config.bindAddress());
        assertEquals(1965, config.port());
        assertNull(config.certPath());
        assertNull(config.keyPath());
    }

    @Test
    void builderWithAllOptions() {
        Path cert = Path.of("cert.pem");
        Path key = Path.of("key.pem");

        GembootConfig config = GembootConfig.builder()
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
        GembootConfig fromBuilder = GembootConfig.builder().build();
        GembootConfig fromDefaults = GembootConfig.defaults();

        assertEquals(fromDefaults.hostname(), fromBuilder.hostname());
        assertEquals(fromDefaults.port(), fromBuilder.port());
        assertEquals(fromDefaults.certPath(), fromBuilder.certPath());
        assertEquals(fromDefaults.keyPath(), fromBuilder.keyPath());
    }

    @Test
    void builderRejectsCertWithoutKey() {
        assertThrows(IllegalArgumentException.class, () ->
                GembootConfig.builder()
                        .certificate(Path.of("cert.pem"), null)
                        .build());
    }

    @Test
    void builderRejectsKeyWithoutCert() {
        assertThrows(IllegalArgumentException.class, () ->
                GembootConfig.builder()
                        .certificate(null, Path.of("key.pem"))
                        .build());
    }

    @Test
    void fromPropertiesWithAllValues() throws IOException {
        Path propsFile = tempDir.resolve("gemboot.properties");
        Files.writeString(propsFile, """
                gemboot.hostname=example.com
                gemboot.port=1966
                gemboot.cert.path=/etc/gemini/cert.pem
                gemboot.key.path=/etc/gemini/key.pem
                """);

        GembootConfig config = GembootConfig.fromProperties(propsFile);

        assertEquals("example.com", config.hostname());
        assertEquals(1966, config.port());
        assertEquals(Path.of("/etc/gemini/cert.pem"), config.certPath());
        assertEquals(Path.of("/etc/gemini/key.pem"), config.keyPath());
    }

    @Test
    void fromPropertiesWithDefaults() throws IOException {
        Path propsFile = tempDir.resolve("gemboot.properties");
        Files.writeString(propsFile, "");

        GembootConfig config = GembootConfig.fromProperties(propsFile);

        assertEquals("localhost", config.hostname());
        assertEquals(1965, config.port());
        assertNull(config.certPath());
        assertNull(config.keyPath());
    }

    @Test
    void fromPropertiesPartialOverride() throws IOException {
        Path propsFile = tempDir.resolve("gemboot.properties");
        Files.writeString(propsFile, "gemboot.hostname=myhost.com\n");

        GembootConfig config = GembootConfig.fromProperties(propsFile);

        assertEquals("myhost.com", config.hostname());
        assertEquals(1965, config.port());
    }

    @Test
    void fromPropertiesRejectsCertWithoutKey() throws IOException {
        Path propsFile = tempDir.resolve("gemboot.properties");
        Files.writeString(propsFile, "gemboot.cert.path=/cert.pem\n");

        assertThrows(IllegalArgumentException.class,
                () -> GembootConfig.fromProperties(propsFile));
    }

    @Test
    void fromPropertiesRejectsKeyWithoutCert() throws IOException {
        Path propsFile = tempDir.resolve("gemboot.properties");
        Files.writeString(propsFile, "gemboot.key.path=/key.pem\n");

        assertThrows(IllegalArgumentException.class,
                () -> GembootConfig.fromProperties(propsFile));
    }

    @Test
    void fromPropertiesRejectsMissingFile() {
        assertThrows(IllegalArgumentException.class,
                () -> GembootConfig.fromProperties(tempDir.resolve("nope.properties")));
    }

    @Test
    void fromPropertiesDefaultPathReadsFromWorkingDirectory() {
        GembootConfig config = GembootConfig.fromProperties();
        assertEquals("testhost.local", config.hostname());
        assertEquals(1966, config.port());
        assertNull(config.certPath());
        assertNull(config.keyPath());
    }
}
