package io.github.wallawood;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WallawoodConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsUsesLocalhostAnd1965() {
        WallawoodConfig config = WallawoodConfig.defaults();
        assertEquals("localhost", config.hostname());
        assertEquals("0.0.0.0", config.bindAddress());
        assertEquals(1965, config.port());
        assertNull(config.certPath());
        assertNull(config.keyPath());
        assertTrue(config.staticDirectories().isEmpty());
    }

    @Test
    void builderWithAllOptions() {
        Path cert = Path.of("cert.pem");
        Path key = Path.of("key.pem");

        WallawoodConfig config = WallawoodConfig.builder()
                .hostname("example.com")
                .port(1966)
                .certificate(cert, key)
                .staticDirectories(List.of("classpath:/public", "file:./content"))
                .build();

        assertEquals("example.com", config.hostname());
        assertEquals(1966, config.port());
        assertEquals(cert, config.certPath());
        assertEquals(key, config.keyPath());
        assertEquals(List.of("classpath:/public", "file:./content"), config.staticDirectories());
    }

    @Test
    void builderDefaultsMatchDefaults() {
        WallawoodConfig fromBuilder = WallawoodConfig.builder().build();
        WallawoodConfig fromDefaults = WallawoodConfig.defaults();

        assertEquals(fromDefaults.hostname(), fromBuilder.hostname());
        assertEquals(fromDefaults.port(), fromBuilder.port());
        assertEquals(fromDefaults.certPath(), fromBuilder.certPath());
        assertEquals(fromDefaults.keyPath(), fromBuilder.keyPath());
        assertEquals(fromDefaults.staticDirectories(), fromBuilder.staticDirectories());
    }

    @Test
    void builderRejectsCertWithoutKey() {
        assertThrows(IllegalArgumentException.class, () ->
                WallawoodConfig.builder()
                        .certificate(Path.of("cert.pem"), null)
                        .build());
    }

    @Test
    void builderRejectsKeyWithoutCert() {
        assertThrows(IllegalArgumentException.class, () ->
                WallawoodConfig.builder()
                        .certificate(null, Path.of("key.pem"))
                        .build());
    }

    @Test
    void fromPropertiesWithAllValues() throws IOException {
        Path propsFile = tempDir.resolve("application.properties");
        Files.writeString(propsFile, """
                wallawood.hostname=example.com
                wallawood.port=1966
                wallawood.cert.path=/etc/gemini/cert.pem
                wallawood.key.path=/etc/gemini/key.pem
                """);

        WallawoodConfig config = WallawoodConfig.fromProperties(propsFile);

        assertEquals("example.com", config.hostname());
        assertEquals(1966, config.port());
        assertEquals(Path.of("/etc/gemini/cert.pem"), config.certPath());
        assertEquals(Path.of("/etc/gemini/key.pem"), config.keyPath());
    }

    @Test
    void fromPropertiesWithDefaults() throws IOException {
        Path propsFile = tempDir.resolve("application.properties");
        Files.writeString(propsFile, "");

        WallawoodConfig config = WallawoodConfig.fromProperties(propsFile);

        assertEquals("localhost", config.hostname());
        assertEquals(1965, config.port());
        assertNull(config.certPath());
        assertNull(config.keyPath());
        assertTrue(config.staticDirectories().isEmpty());
    }

    @Test
    void fromPropertiesPartialOverride() throws IOException {
        Path propsFile = tempDir.resolve("application.properties");
        Files.writeString(propsFile, "wallawood.hostname=myhost.com\n");

        WallawoodConfig config = WallawoodConfig.fromProperties(propsFile);

        assertEquals("myhost.com", config.hostname());
        assertEquals(1965, config.port());
    }

    @Test
    void fromPropertiesRejectsCertWithoutKey() throws IOException {
        Path propsFile = tempDir.resolve("application.properties");
        Files.writeString(propsFile, "wallawood.cert.path=/cert.pem\n");

        assertThrows(IllegalArgumentException.class,
                () -> WallawoodConfig.fromProperties(propsFile));
    }

    @Test
    void fromPropertiesRejectsKeyWithoutCert() throws IOException {
        Path propsFile = tempDir.resolve("application.properties");
        Files.writeString(propsFile, "wallawood.key.path=/key.pem\n");

        assertThrows(IllegalArgumentException.class,
                () -> WallawoodConfig.fromProperties(propsFile));
    }

    @Test
    void fromPropertiesRejectsMissingFile() {
        assertThrows(IllegalArgumentException.class,
                () -> WallawoodConfig.fromProperties(tempDir.resolve("nope.properties")));
    }

    @Test
    void fromPropertiesDefaultPathReadsFromWorkingDirectory() {
        WallawoodConfig config = WallawoodConfig.fromProperties();
        assertEquals("testhost.local", config.hostname());
        assertEquals(1966, config.port());
        assertNull(config.certPath());
        assertNull(config.keyPath());
    }

    @Test
    void fromPropertiesParsesStaticDirectories() throws IOException {
        Path propsFile = tempDir.resolve("application.properties");
        Files.writeString(propsFile, "wallawood.static.directories=classpath:/public,file:./content\n");

        WallawoodConfig config = WallawoodConfig.fromProperties(propsFile);

        assertEquals(List.of("classpath:/public", "file:./content"), config.staticDirectories());
    }

    @Test
    void fromPropertiesParsesStaticDirectoriesWithSpaces() throws IOException {
        Path propsFile = tempDir.resolve("application.properties");
        Files.writeString(propsFile, "wallawood.static.directories= classpath:/public , file:./content \n");

        WallawoodConfig config = WallawoodConfig.fromProperties(propsFile);

        assertEquals(List.of("classpath:/public", "file:./content"), config.staticDirectories());
    }

    @Test
    void fromPropertiesEmptyStaticDirectories() throws IOException {
        Path propsFile = tempDir.resolve("application.properties");
        Files.writeString(propsFile, "wallawood.static.directories=\n");

        WallawoodConfig config = WallawoodConfig.fromProperties(propsFile);

        assertTrue(config.staticDirectories().isEmpty());
    }

    @Test
    void staticDirectoriesListIsUnmodifiable() {
        WallawoodConfig config = WallawoodConfig.builder()
                .staticDirectories(List.of("classpath:/public"))
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> config.staticDirectories().add("classpath:/other"));
    }
}
