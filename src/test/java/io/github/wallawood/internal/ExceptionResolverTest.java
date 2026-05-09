package io.github.wallawood.internal;

import org.junit.jupiter.api.Test;
import io.github.wallawood.annotations.GeminiExceptionHandler;
import io.github.wallawood.GeminiResponse;
import io.github.wallawood.annotations.GeminiController;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionResolverTest {

    @GeminiExceptionHandler
    static class TestErrorHandler {

        public GeminiResponse handle(IllegalArgumentException e) {
            return GeminiResponse.badRequest(e.getMessage());
        }

        public GeminiResponse handle(RuntimeException e) {
            return GeminiResponse.temporaryFailure("Runtime: " + e.getMessage());
        }

        public GeminiResponse handle(Exception ignored) {
            return GeminiResponse.permanentFailure("Catch-all");
        }
    }

    @Test
    void matchesMostSpecificException() {
        var resolver = new ExceptionResolver(new TestErrorHandler());
        var result = resolver.resolve(new IllegalArgumentException("bad input"));
        assertEquals(59, result.status());
        assertEquals("bad input", result.meta());
    }

    @Test
    void matchesParentExceptionType() {
        var resolver = new ExceptionResolver(new TestErrorHandler());
        var result = resolver.resolve(new NullPointerException("npe"));
        assertEquals(40, result.status());
        assertTrue(result.meta().contains("Runtime: npe"));
    }

    @Test
    void matchesCatchAll() {
        var resolver = new ExceptionResolver(new TestErrorHandler());
        var result = resolver.resolve(new java.io.IOException("io"));
        assertEquals(50, result.status());
        assertEquals("Catch-all", result.meta());
    }

    @Test
    void noneResolverReturnsNull() {
        var resolver = ExceptionResolver.none();
        assertNull(resolver.resolve(new RuntimeException("test")));
    }

    @Test
    void handlerInvokerUsesResolver() throws Exception {
        var resolver = new ExceptionResolver(new TestErrorHandler());

        var controller = new ThrowingController();
        var method = ThrowingController.class.getDeclaredMethod("boom");
        var handler = new HandlerMethod(controller, method);
        var matched = new RouteRegistry.MatchedRoute(handler, java.util.Map.of());

        var ctx = new io.github.wallawood.RequestContext();
        ctx.add(java.net.URI.create("gemini://localhost/boom"));
        var result = HandlerInvoker.invoke(matched, ctx, resolver);
        assertEquals(40, result.status());
        assertTrue(result.meta().contains("Runtime: kaboom"));
    }

    @GeminiController
    static class ThrowingController {
        @io.github.wallawood.annotations.Path("/boom")
        public GeminiResponse boom() {
            throw new RuntimeException("kaboom");
        }
    }
}
