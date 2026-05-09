package io.github.wallawood.internal;

import io.github.wallawood.Grant;
import io.github.wallawood.RequestContext;
import io.github.wallawood.annotations.RequireAuthorized;
import io.github.wallawood.annotations.RequireClearance;
import io.github.wallawood.annotations.RequireScopes;
import io.github.wallawood.annotations.DefaultValue;
import io.github.wallawood.annotations.Path;
import io.github.wallawood.annotations.PathParam;
import io.github.wallawood.annotations.QueryParam;
import io.github.wallawood.annotations.Context;
import org.junit.jupiter.api.Test;
import io.github.wallawood.annotations.GeminiController;
import io.github.wallawood.GeminiResponse;
import io.github.wallawood.annotations.QueryString;
import io.github.wallawood.annotations.RequireCertificate;
import io.github.wallawood.annotations.RequireInput;
import io.github.wallawood.annotations.RequireSensitiveInput;
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

        @Path("/raw-int")
        public GeminiResponse rawInt(@QueryString int value) {
            return GeminiResponse.success("int " + value);
        }

        @Path("/raw-bool")
        public GeminiResponse rawBool(@QueryString boolean flag) {
            return GeminiResponse.success("bool " + flag);
        }

        @Path("/auth-simple")
        @RequireAuthorized
        public GeminiResponse authSimple() {
            return GeminiResponse.success("authorized");
        }

        @Path("/auth-level")
        @RequireClearance(level = 3, message = "Admins only")
        public GeminiResponse authLevel() {
            return GeminiResponse.success("admin");
        }

        @Path("/auth-scope")
        @RequireScopes(scopes = "notes:write", message = "Missing scope")
        public GeminiResponse authScope() {
            return GeminiResponse.success("scoped");
        }

        @Path("/auth-multi-scope")
        @RequireScopes(scopes = {"notes:read", "notes:write"})
        public GeminiResponse authMultiScope() {
            return GeminiResponse.success("multi-scoped");
        }

        @Path("/auth-level-and-scope")
        @RequireClearance(level = 2)
        @RequireScopes(scopes = "admin:write", message = "Insufficient permissions")
        public GeminiResponse authLevelAndScope() {
            return GeminiResponse.success("level+scope");
        }

        @Path("/auth-all-three")
        @RequireAuthorized
        @RequireClearance(level = 3)
        @RequireScopes(scopes = "super:admin")
        public GeminiResponse authAllThree() {
            return GeminiResponse.success("all-three");
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

    @GeminiController
    @RequireClearance(level = 2, message = "Members only")
    static class AuthorizedController {

        @Path("/members")
        public GeminiResponse members() {
            return GeminiResponse.success("members area");
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

    private RequestContext ctx(URI uri) {
        var rc = new RequestContext();
        rc.add(uri);
        return rc;
    }

    private RequestContext ctx(URI uri, Grant grant) {
        var rc = ctx(uri);
        rc.add(grant);
        return rc;
    }

    private RequestContext ctxWithCert(URI uri, Grant grant) {
        var rc = ctx(uri);
        rc.add(X509Certificate.class, stubCert());
        rc.add(grant);
        return rc;
    }

    private static X509Certificate stubCert() {
        try {
            var kpg = java.security.KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            var kp = kpg.generateKeyPair();
            var name = new org.bouncycastle.asn1.x500.X500Name("CN=test");
            var now = java.time.Instant.now();
            var builder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                    name, java.math.BigInteger.ONE,
                    java.util.Date.from(now), java.util.Date.from(now.plusSeconds(3600)),
                    name, kp.getPublic());
            var signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withECDSA")
                    .build(kp.getPrivate());
            return new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                    .getCertificate(builder.build(signer));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void invokesSimpleHandler() throws Exception {
        var handler = handlerFor(new TestController(), "hello");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/hello")), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void resolvesPathParam() throws Exception {
        var handler = handlerFor(new TestController(), "user");
        var result = HandlerInvoker.invoke(matched(handler, Map.of("id", "42")), ctx(URI.create("gemini://localhost/user/42")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("user 42"));
    }

    @Test
    void resolvesQueryParam() throws Exception {
        var handler = handlerFor(new TestController(), "search");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/search?q=gemini")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("search gemini"));
    }

    @Test
    void appliesDefaultValue() throws Exception {
        var handler = handlerFor(new TestController(), "paged");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/paged")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("page 1"));
    }

    @Test
    void defaultValueOverriddenByQuery() throws Exception {
        var handler = handlerFor(new TestController(), "paged");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/paged?page=5")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("page 5"));
    }

    @Test
    void exceptionReturnsTemporaryFailure() throws Exception {
        var handler = handlerFor(new TestController(), "boom");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/boom")), ExceptionResolver.none());
        assertEquals(40, result.status());
    }

    @Test
    void contextInjectsRequestUri() throws Exception {
        var handler = handlerFor(new TestController(), "withUri");
        var uri = URI.create("gemini://localhost/uri");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(uri), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("gemini://localhost/uri"));
    }

    @Test
    void contextInjectsNullCertWhenMissing() throws Exception {
        var handler = handlerFor(new TestController(), "certInfo");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/cert-info")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("no cert"));
    }

    @Test
    void uriWithoutContextAnnotationIsNull() throws Exception {
        var handler = handlerFor(new TestController(), "hello");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/hello")), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void requireCertificateOnMethodReturns60WithMessage() throws Exception {
        var handler = handlerFor(new TestController(), "protectedRoute");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/protected")), ExceptionResolver.none());
        assertEquals(60, result.status());
        assertEquals("Certificate needed", result.meta());
    }

    @Test
    void requireCertificateOnClassReturns60WithMessage() throws Exception {
        var handler = handlerFor(new ProtectedController(), "route");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/all-protected")), ExceptionResolver.none());
        assertEquals(60, result.status());
        assertEquals("Login required", result.meta());
    }

    @Test
    void decodesPercentEncodedQueryParam() throws Exception {
        var handler = handlerFor(new TestController(), "search");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/search?q=hello%20world")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("search hello world"));
    }

    @Test
    void queryStringCapturesRawInput() throws Exception {
        var handler = handlerFor(new TestController(), "rawSearch");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/raw-search?hello%20world")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("raw hello world"));
    }

    @Test
    void queryStringDecodesPercentEncoding() throws Exception {
        var handler = handlerFor(new TestController(), "rawSearch");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/raw-search?caf%C3%A9")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("raw café"));
    }

    @Test
    void queryStringUsesDefaultWhenMissing() throws Exception {
        var handler = handlerFor(new TestController(), "rawDefault");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/raw-default")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("raw none"));
    }

    @Test
    void queryStringOverridesDefault() throws Exception {
        var handler = handlerFor(new TestController(), "rawDefault");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/raw-default?42")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("raw 42"));
    }

    @Test
    void queryStringCoercesToInt() throws Exception {
        var handler = handlerFor(new TestController(), "rawInt");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/raw-int?99")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("int 99"));
    }

    @Test
    void queryStringCoercesToBoolean() throws Exception {
        var handler = handlerFor(new TestController(), "rawBool");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/raw-bool?true")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("bool true"));
    }

    @Test
    void queryStringInvalidIntReturnsBadRequest() throws Exception {
        var handler = handlerFor(new TestController(), "rawInt");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/raw-int?abc")), ExceptionResolver.none());
        assertEquals(59, result.status());
    }

    @Test
    void convertsPathParamToInt() throws Exception {
        var handler = handlerFor(new TestController(), "userInt");
        var result = HandlerInvoker.invoke(matched(handler, Map.of("id", "99")), ctx(URI.create("gemini://localhost/user-int/99")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("user 99"));
    }

    @Test
    void convertsQueryParamToIntWithDefault() throws Exception {
        var handler = handlerFor(new TestController(), "page");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/page")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("page 1"));
    }

    @Test
    void convertsQueryParamToIntFromQuery() throws Exception {
        var handler = handlerFor(new TestController(), "page");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/page?num=5")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("page 5"));
    }

    @Test
    void convertsBooleanQueryParam() throws Exception {
        var handler = handlerFor(new TestController(), "flag");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/flag?active=true")), ExceptionResolver.none());
        assertTrue(new String(result.body()).contains("active true"));
    }

    @Test
    void invalidIntParamReturnsBadRequest() throws Exception {
        var handler = handlerFor(new TestController(), "userInt");
        var result = HandlerInvoker.invoke(matched(handler, Map.of("id", "abc")), ctx(URI.create("gemini://localhost/user-int/abc")), ExceptionResolver.none());
        assertEquals(59, result.status());
        assertTrue(result.meta().contains("Invalid value"));
    }

    @Test
    void requireInputReturns10WhenQueryMissing() throws Exception {
        var handler = handlerFor(new TestController(), "searchInput");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/search-input")), ExceptionResolver.none());
        assertEquals(10, result.status());
        assertEquals("Enter a search term:", result.meta());
    }

    @Test
    void requireInputProceedsWhenQueryPresent() throws Exception {
        var handler = handlerFor(new TestController(), "searchInput");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/search-input?gemini")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("found gemini"));
    }

    @Test
    void requireSensitiveInputReturns11WhenQueryMissing() throws Exception {
        var handler = handlerFor(new TestController(), "login");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/login")), ExceptionResolver.none());
        assertEquals(11, result.status());
        assertEquals("Enter password:", result.meta());
    }

    @Test
    void requireSensitiveInputProceedsWhenQueryPresent() throws Exception {
        var handler = handlerFor(new TestController(), "login");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/login?secret123")), ExceptionResolver.none());
        assertEquals(20, result.status());
        assertTrue(new String(result.body()).contains("logged in"));
    }

    // --- @RequireAuthorized tests ---

    @Test
    void authorizeSimplePassesWithGrant() throws Exception {
        var handler = handlerFor(new TestController(), "authSimple");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-simple"), Grant.authorized()), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void authorizeReturns60WhenNoCert() throws Exception {
        var handler = handlerFor(new TestController(), "authSimple");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/auth-simple")), ExceptionResolver.none());
        assertEquals(60, result.status());
    }

    @Test
    void authorizeReturns61WhenCertPresentButNoneGrant() throws Exception {
        var handler = handlerFor(new TestController(), "authSimple");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-simple"), Grant.none()), ExceptionResolver.none());
        assertEquals(61, result.status());
    }

    // --- @RequireClearance tests ---

    @Test
    void clearanceLevelPassesWhenSufficient() throws Exception {
        var handler = handlerFor(new TestController(), "authLevel");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-level"), Grant.clearance(5)), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void clearanceLevelPassesWhenExact() throws Exception {
        var handler = handlerFor(new TestController(), "authLevel");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-level"), Grant.clearance(3)), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void clearanceLevelFailsWhenInsufficient() throws Exception {
        var handler = handlerFor(new TestController(), "authLevel");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-level"), Grant.clearance(2)), ExceptionResolver.none());
        assertEquals(61, result.status());
        assertEquals("Admins only", result.meta());
    }

    // --- @RequireScopes tests ---

    @Test
    void scopePassesWhenPresent() throws Exception {
        var handler = handlerFor(new TestController(), "authScope");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-scope"), Grant.scopes("notes:write", "notes:read")), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void scopeFailsWhenMissing() throws Exception {
        var handler = handlerFor(new TestController(), "authScope");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-scope"), Grant.scopes("notes:read")), ExceptionResolver.none());
        assertEquals(61, result.status());
        assertEquals("Missing scope", result.meta());
    }

    @Test
    void multiScopePassesWhenAllPresent() throws Exception {
        var handler = handlerFor(new TestController(), "authMultiScope");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-multi-scope"), Grant.scopes("notes:read", "notes:write", "admin")), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void multiScopeFailsWhenPartial() throws Exception {
        var handler = handlerFor(new TestController(), "authMultiScope");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-multi-scope"), Grant.scopes("notes:read")), ExceptionResolver.none());
        assertEquals(61, result.status());
    }

    @Test
    void authorizedGrantDoesNotBypassClearanceCheck() throws Exception {
        var handler = handlerFor(new TestController(), "authLevel");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-level"), Grant.authorized()), ExceptionResolver.none());
        assertEquals(61, result.status());
    }

    // --- Class-level annotation tests ---

    @Test
    void clearanceOnClassReturns60WhenNoCert() throws Exception {
        var handler = handlerFor(new AuthorizedController(), "members");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctx(URI.create("gemini://localhost/members")), ExceptionResolver.none());
        assertEquals(60, result.status());
        assertEquals("Members only", result.meta());
    }

    @Test
    void clearanceOnClassPassesWithSufficientLevel() throws Exception {
        var handler = handlerFor(new AuthorizedController(), "members");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/members"), Grant.clearance(2)), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    // --- AND semantics: multiple annotations on one method ---

    @Test
    void andSemanticsPassesWhenBothClearanceAndScopesMet() throws Exception {
        var handler = handlerFor(new TestController(), "authLevelAndScope");
        var grant = Grant.builder().authorized(true).clearance(3).addScope("admin:write").build();
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-level-and-scope"), grant), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void andSemanticsFailsWhenOnlyClearanceMet() throws Exception {
        var handler = handlerFor(new TestController(), "authLevelAndScope");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-level-and-scope"), Grant.clearance(5)), ExceptionResolver.none());
        assertEquals(61, result.status());
    }

    @Test
    void andSemanticsFailsWhenOnlyScopesMet() throws Exception {
        var handler = handlerFor(new TestController(), "authLevelAndScope");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-level-and-scope"), Grant.scopes("admin:write")), ExceptionResolver.none());
        assertEquals(61, result.status());
    }

    @Test
    void andSemanticsAllThreePassesWithFullGrant() throws Exception {
        var handler = handlerFor(new TestController(), "authAllThree");
        var grant = Grant.builder().authorized(true).clearance(5).addScope("super:admin").build();
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-all-three"), grant), ExceptionResolver.none());
        assertEquals(20, result.status());
    }

    @Test
    void andSemanticsAllThreeFailsWithoutAuthorized() throws Exception {
        var handler = handlerFor(new TestController(), "authAllThree");
        var grant = Grant.builder().clearance(5).addScope("super:admin").build();
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-all-three"), grant), ExceptionResolver.none());
        assertEquals(61, result.status());
    }

    @Test
    void andSemanticsAllThreeFailsWithoutClearance() throws Exception {
        var handler = handlerFor(new TestController(), "authAllThree");
        var grant = Grant.builder().authorized(true).addScope("super:admin").build();
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-all-three"), grant), ExceptionResolver.none());
        assertEquals(61, result.status());
    }

    @Test
    void andSemanticsAllThreeFailsWithoutScopes() throws Exception {
        var handler = handlerFor(new TestController(), "authAllThree");
        var grant = Grant.builder().authorized(true).clearance(5).build();
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-all-three"), grant), ExceptionResolver.none());
        assertEquals(61, result.status());
    }

    @Test
    void andSemanticsUsesLastAnnotationMessage() throws Exception {
        var handler = handlerFor(new TestController(), "authLevelAndScope");
        var result = HandlerInvoker.invoke(matched(handler, Map.of()), ctxWithCert(URI.create("gemini://localhost/auth-level-and-scope"), Grant.clearance(1)), ExceptionResolver.none());
        assertEquals(61, result.status());
        assertEquals("Insufficient permissions", result.meta());
    }
}
