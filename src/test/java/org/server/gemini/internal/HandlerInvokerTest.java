package org.server.gemini.internal;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import org.junit.jupiter.api.Test;
import org.server.gemini.annotations.GeminiController;
import org.server.gemini.GeminiResponse;
import org.server.gemini.annotations.QueryString;
import org.server.gemini.annotations.RequireCertificate;
import org.server.gemini.annotations.RequireInput;
import org.server.gemini.annotations.RequireSensitiveInput;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandlerInvokerTest {

    @GeminiController
    static class TestController {

        @Path("/hello")
        public GeminiResponse hello() {
            return GeminiResponse.success("hello");
        }

        @Path("/user/{id}")
        public GeminiResponse user(@PathParam("id") String id) {
            return GeminiResponse.success("user " + id);
        }

        @Path("/search")
        public GeminiResponse search(@QueryParam("q") String q) {
            return GeminiResponse.success("search " + q);
        }

        @Path("/paged")
        public GeminiResponse paged(@QueryParam("page") @DefaultValue("1") String page) {
            return GeminiResponse.success("page " + page);
        }

        @Path("/boom")
        public GeminiResponse boom() {
            throw new RuntimeException("kaboom");
        }

        @Path("/uri")
        public GeminiResponse withUri(@Context URI uri) {
            return GeminiResponse.success(uri.toString());
        }

        @Path("/protected")
        @RequireCertificate("Certificate needed")
        public GeminiResponse protectedRoute() {
            return GeminiResponse.success("secret");
        }

        @Path("/cert-info")
        public GeminiResponse certInfo(@Context X509Certificate cert) {
            return GeminiResponse.success(cert != null ? "has cert" : "no cert");
        }

        @Path("/user-int/{id}")
        public GeminiResponse userInt(@PathParam("id") int id) {
            return GeminiResponse.success("user " + id);
        }

        @Path("/page")
        public GeminiResponse page(@QueryParam("num") @DefaultValue("1") int num) {
            return GeminiResponse.success("page " + num);
        }

        @Path("/flag")
        public GeminiResponse flag(@QueryParam("active") boolean active) {
            return GeminiResponse.success("active " + active);
        }

        @Path("/search-input")
        @RequireInput("Enter a search term:")
        public GeminiResponse searchInput(@QueryString String input) {
            return GeminiResponse.success("found " + input);
        }

        @Path("/login")
        @RequireSensitiveInput("Enter password:")
        public GeminiResponse login(@QueryString String password) {
            return GeminiResponse.success("logged in");
        }

        @Path("/raw-search")
        public GeminiResponse rawSearch(@QueryString String input) {
            return GeminiResponse.success("raw " + input);
        }

        @Path("/raw-default")
        public GeminiResponse rawDefault(@QueryString @DefaultValue("none") String input) {
            return GeminiResponse.success("raw " + input);
        }
    }

    @GeminiController
    @RequireCertificate("Login required")
    static class ProtectedController {

        @Path("/all-protected")
        public GeminiResponse route() {
            return GeminiResponse.success("secret");
        }
    }

    private HandlerMethod handlerFor(Object controller, String methodName) throws Exception {
        for (var m : controller.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return new HandlerMethod(controller, m);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private RouteRegistry.MatchedRoute matched(HandlerMethod handler, Map<String, String> pathVars) {
        return new RouteRegistry.MatchedRoute(handler, pathVars);
    }

    @Test
    void invokesSimpleHandler() throws Exception {
        var handler = handlerFor(new TestController(), "hello");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/hello"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void resolvesPathParam() throws Exception {
        var handler = handlerFor(new TestController(), "user");
        var result = HandlerInvoker.invoke(matched(handler, Map.of("id", "42")), URI.create("gemini://localhost/user/42"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("user 42"));
    }

    @Test
    void resolvesQueryParam() throws Exception {
        var handler = handlerFor(new TestController(), "search");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/search?q=gemini"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("search gemini"));
    }

    @Test
    void appliesDefaultValue() throws Exception {
        var handler = handlerFor(new TestController(), "paged");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/paged"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("page 1"));
    }

    @Test
    void defaultValueOverriddenByQuery() throws Exception {
        var handler = handlerFor(new TestController(), "paged");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/paged?page=5"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("page 5"));
    }

    @Test
    void exceptionReturnsTemporaryFailure() throws Exception {
        var handler = handlerFor(new TestController(), "boom");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/boom"), null, ExceptionResolver.none());
        assertEquals(40, result.status());
    }

    @Test
    void contextInjectsRequestUri() throws Exception {
        var handler = handlerFor(new TestController(), "withUri");
        var uri = URI.create("gemini://localhost/uri");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), uri, null, ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("gemini://localhost/uri"));
    }

    @Test
    void contextInjectsNullCertWhenMissing() throws Exception {
        var handler = handlerFor(new TestController(), "certInfo");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/cert-info"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("no cert"));
    }

    @Test
    void uriWithoutContextAnnotationIsNull() throws Exception {
        // A URI param without @Context should not be injected
        var handler = handlerFor(new TestController(), "hello");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/hello"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void requireCertificateOnMethodReturns60WithMessage() throws Exception {
        var handler = handlerFor(new TestController(), "protectedRoute");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/protected"), null, ExceptionResolver.none());
        assertEquals(60, result.status());
        assertEquals("Certificate needed", result.meta());
    }

    @Test
    void requireCertificateOnClassReturns60WithMessage() throws Exception {
        var handler = handlerFor(new ProtectedController(), "route");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/all-protected"), null, ExceptionResolver.none());
        assertEquals(60, result.status());
        assertEquals("Login required", result.meta());
    }

    @Test
    void decodesPercentEncodedQueryParam() throws Exception {
        var handler = handlerFor(new TestController(), "search");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/search?q=hello%20world"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("search hello world"));
    }

    @Test
    void queryStringCapturesRawInput() throws Exception {
        var handler = handlerFor(new TestController(), "rawSearch");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/raw-search?hello%20world"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("raw hello world"));
    }

    @Test
    void queryStringDecodesPercentEncoding() throws Exception {
        var handler = handlerFor(new TestController(), "rawSearch");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/raw-search?caf%C3%A9"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("raw café"));
    }

    @Test
    void queryStringUsesDefaultWhenMissing() throws Exception {
        var handler = handlerFor(new TestController(), "rawDefault");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/raw-default"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("raw none"));
    }

    @Test
    void queryStringOverridesDefault() throws Exception {
        var handler = handlerFor(new TestController(), "rawDefault");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/raw-default?42"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("raw 42"));
    }

    @Test
    void convertsPathParamToInt() throws Exception {
        var handler = handlerFor(new TestController(), "userInt");
        var result = HandlerInvoker.invoke(matched(handler, Map.of("id", "99")), URI.create("gemini://localhost/user-int/99"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("user 99"));
    }

    @Test
    void convertsQueryParamToIntWithDefault() throws Exception {
        var handler = handlerFor(new TestController(), "page");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/page"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("page 1"));
    }

    @Test
    void convertsQueryParamToIntFromQuery() throws Exception {
        var handler = handlerFor(new TestController(), "page");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/page?num=5"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("page 5"));
    }

    @Test
    void convertsBooleanQueryParam() throws Exception {
        var handler = handlerFor(new TestController(), "flag");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/flag?active=true"), null, ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("active true"));
    }

    @Test
    void invalidIntParamReturnsBadRequest() throws Exception {
        var handler = handlerFor(new TestController(), "userInt");
        var result = HandlerInvoker.invoke(matched(handler, Map.of("id", "abc")), URI.create("gemini://localhost/user-int/abc"), null, ExceptionResolver.none());
        assertEquals(59, result.status());
        assertTrue(result.meta().contains("Invalid value"));
    }

    @Test
    void requireInputReturns10WhenQueryMissing() throws Exception {
        var handler = handlerFor(new TestController(), "searchInput");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/search-input"), null, ExceptionResolver.none());
        assertEquals(10, result.status());
        assertEquals("Enter a search term:", result.meta());
    }

    @Test
    void requireInputProceedsWhenQueryPresent() throws Exception {
        var handler = handlerFor(new TestController(), "searchInput");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/search-input?gemini"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("found gemini"));
    }

    @Test
    void requireSensitiveInputReturns11WhenQueryMissing() throws Exception {
        var handler = handlerFor(new TestController(), "login");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/login"), null, ExceptionResolver.none());
        assertEquals(11, result.status());
        assertEquals("Enter password:", result.meta());
    }

    @Test
    void requireSensitiveInputProceedsWhenQueryPresent() throws Exception {
        var handler = handlerFor(new TestController(), "login");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), URI.create("gemini://localhost/login?secret123"), null, ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("logged in"));
    }
}
