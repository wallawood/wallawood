package io.github.wallawood.internal;

import io.github.wallawood.GeminiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaticResourceResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void servesGemtextFile() {
        GeminiResponse r = StaticResourceResolver.resolve("/docs/guide.gmi");
        assertNotNull(r);
        assertEquals(20, r.status());
        assertEquals("text/gemini; charset=utf-8", r.meta());
        assertTrue(new String(r.body()).contains("# Guide"));
    }

    @Test
    void servesIndexGmiForDirectoryPath() {
        GeminiResponse r = StaticResourceResolver.resolve("/");
        assertNotNull(r);
        assertEquals(20, r.status());
        assertTrue(new String(r.body()).contains("# Welcome"));
    }

    @Test
    void servesIndexGmiForSubdirectory() {
        GeminiResponse r = StaticResourceResolver.resolve("/docs/");
        assertNotNull(r);
        assertEquals(20, r.status());
        assertTrue(new String(r.body()).contains("# Docs Index"));
    }

    @Test
    void servesIndexGmiForSubdirectoryWithoutTrailingSlash() {
        GeminiResponse r = StaticResourceResolver.resolve("/docs");
        assertNotNull(r);
        assertEquals(20, r.status());
        assertTrue(new String(r.body()).contains("# Docs Index"));
    }

    @Test
    void servesGmiExtensionFallbackForCleanUrl() {
        GeminiResponse r = StaticResourceResolver.resolve("/docs/guide");
        assertNotNull(r);
        assertEquals(20, r.status());
        assertEquals("text/gemini; charset=utf-8", r.meta());
        assertTrue(new String(r.body()).contains("# Guide"));
    }

    @Test
    void servesPlainTextFile() {
        GeminiResponse r = StaticResourceResolver.resolve("/readme.txt");
        assertNotNull(r);
        assertEquals(20, r.status());
        assertEquals("text/plain", r.meta());
        assertTrue(new String(r.body()).contains("plain text file"));
    }

    @Test
    void servesBinaryFile() {
        GeminiResponse r = StaticResourceResolver.resolve("/logo.png");
        assertNotNull(r);
        assertEquals(20, r.status());
        assertEquals("image/png", r.meta());
        assertNotNull(r.body());
        assertTrue(r.body().length > 0);
    }

    @Test
    void returnsNullForMissingResource() {
        GeminiResponse r = StaticResourceResolver.resolve("/nonexistent.gmi");
        assertNull(r);
    }

    @Test
    void returnsNullForMissingDirectory() {
        GeminiResponse r = StaticResourceResolver.resolve("/nope/");
        assertNull(r);
    }

    @Test
    void rejectsPathTraversal() {
        GeminiResponse r = StaticResourceResolver.resolve("/../../../etc/passwd");
        assertNotNull(r);
        assertEquals(59, r.status());
    }

    @Test
    void handlesEmptyPath() {
        GeminiResponse r = StaticResourceResolver.resolve("");
        assertNotNull(r);
        assertEquals(20, r.status());
        assertTrue(new String(r.body()).contains("# Welcome"));
    }

    @Test
    void handlesNullPath() {
        GeminiResponse r = StaticResourceResolver.resolve(null);
        assertNotNull(r);
        assertEquals(20, r.status());
        assertTrue(new String(r.body()).contains("# Welcome"));
    }

    @Test
    void guessMimeTypeForGemini() {
        assertEquals("text/gemini; charset=utf-8", StaticResourceResolver.guessMimeType("page.gmi"));
        assertEquals("text/gemini; charset=utf-8", StaticResourceResolver.guessMimeType("page.gemini"));
    }

    @Test
    void guessMimeTypeForKnownTypes() {
        assertEquals("text/plain", StaticResourceResolver.guessMimeType("file.txt"));
        assertEquals("text/html", StaticResourceResolver.guessMimeType("page.html"));
        assertEquals("image/png", StaticResourceResolver.guessMimeType("logo.png"));
    }

    @Test
    void guessMimeTypeFallsBackToOctetStream() {
        assertEquals("application/octet-stream", StaticResourceResolver.guessMimeType("data.xyz123"));
    }

    @Test
    void servesFromFileDirectory() throws IOException {
        Path contentDir = tempDir.resolve("content");
        Files.createDirectories(contentDir);
        Files.writeString(contentDir.resolve("page.gmi"), "# File Page");

        GeminiResponse r = StaticResourceResolver.resolve("/page.gmi", List.of("file:" + contentDir));
        assertNotNull(r);
        assertEquals(20, r.status());
        assertEquals("# File Page", new String(r.body()));
    }

    @Test
    void classpathStaticTakesPrecedenceOverUserDirectories() throws IOException {
        Path contentDir = tempDir.resolve("content");
        Files.createDirectories(contentDir.resolve("docs"));
        Files.writeString(contentDir.resolve("docs/guide.gmi"), "# Overridden Guide");

        GeminiResponse r = StaticResourceResolver.resolve("/docs/guide.gmi", List.of("file:" + contentDir));
        assertNotNull(r);
        assertTrue(new String(r.body()).contains("# Guide"));
        assertFalse(new String(r.body()).contains("Overridden"));
    }

    @Test
    void fileDirectoryServesWhenClasspathMisses() throws IOException {
        Path contentDir = tempDir.resolve("content");
        Files.createDirectories(contentDir);
        Files.writeString(contentDir.resolve("custom.gmi"), "# Custom");

        GeminiResponse r = StaticResourceResolver.resolve("/custom.gmi", List.of("file:" + contentDir));
        assertNotNull(r);
        assertEquals("# Custom", new String(r.body()));
    }

    @Test
    void firstDirectoryWinsInOrderedList() throws IOException {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        Files.writeString(dir1.resolve("page.gmi"), "# From Dir1");
        Files.writeString(dir2.resolve("page.gmi"), "# From Dir2");

        GeminiResponse r = StaticResourceResolver.resolve("/page.gmi",
                List.of("file:" + dir1, "file:" + dir2));
        assertNotNull(r);
        assertEquals("# From Dir1", new String(r.body()));
    }

    @Test
    void fallsToSecondDirectoryWhenFirstMisses() throws IOException {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        Files.writeString(dir2.resolve("page.gmi"), "# From Dir2");

        GeminiResponse r = StaticResourceResolver.resolve("/page.gmi",
                List.of("file:" + dir1, "file:" + dir2));
        assertNotNull(r);
        assertEquals("# From Dir2", new String(r.body()));
    }

    @Test
    void fileDirectoryServesIndexGmi() throws IOException {
        Path contentDir = tempDir.resolve("content");
        Path subDir = contentDir.resolve("section");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("index.gmi"), "# Section Index");

        GeminiResponse r = StaticResourceResolver.resolve("/section/", List.of("file:" + contentDir));
        assertNotNull(r);
        assertEquals("# Section Index", new String(r.body()));
    }

    @Test
    void fileDirectoryServesGmiExtensionFallback() throws IOException {
        Path contentDir = tempDir.resolve("content");
        Files.createDirectories(contentDir);
        Files.writeString(contentDir.resolve("about.gmi"), "# About");

        GeminiResponse r = StaticResourceResolver.resolve("/about", List.of("file:" + contentDir));
        assertNotNull(r);
        assertEquals("# About", new String(r.body()));
    }

    @Test
    void fileDirectoryRejectsPathTraversal() throws IOException {
        Path contentDir = tempDir.resolve("content");
        Files.createDirectories(contentDir);

        GeminiResponse r = StaticResourceResolver.resolve("/../../../etc/passwd", List.of("file:" + contentDir));
        assertNotNull(r);
        assertEquals(59, r.status());
    }

    @Test
    void classpathDirectoryServesFromUserClasspath() {
        GeminiResponse r = StaticResourceResolver.resolve("/docs/guide.gmi", List.of("classpath:/static"));
        assertNotNull(r);
        assertEquals(20, r.status());
        assertTrue(new String(r.body()).contains("# Guide"));
    }

    @Test
    void returnsNullWhenNoDirectoryHasResource() throws IOException {
        Path contentDir = tempDir.resolve("empty");
        Files.createDirectories(contentDir);

        GeminiResponse r = StaticResourceResolver.resolve("/missing.gmi", List.of("file:" + contentDir));
        assertNull(r);
    }

    // On Windows, Path.normalize().toString() uses backslashes (\), but classpath roots
    // are forward-slash strings. Without converting separators before the startsWith
    // security check, every nested classpath path would be rejected on Windows.

    @Test
    void nestedClasspathPathResolvesOnAllPlatforms() {
        GeminiResponse r = StaticResourceResolver.resolve("/docs/guide.gmi");
        assertNotNull(r, "Nested classpath resource must resolve on all platforms");
        assertEquals(20, r.status());
        assertTrue(new String(r.body()).contains("# Guide"));
    }

    @Test
    void nestedClasspathDirectoryPrefixResolvesOnAllPlatforms() {
        GeminiResponse r = StaticResourceResolver.resolve("/docs/guide.gmi", List.of("classpath:/static"));
        assertNotNull(r, "Nested path via classpath: prefix must resolve on all platforms");
        assertEquals(20, r.status());
        assertTrue(new String(r.body()).contains("# Guide"));
    }
}
