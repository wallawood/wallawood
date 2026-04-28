package org.server.gemini.internal;

import org.junit.jupiter.api.Test;
import org.server.gemini.GeminiResponse;

import static org.junit.jupiter.api.Assertions.*;

class StaticResourceResolverTest {

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
}
