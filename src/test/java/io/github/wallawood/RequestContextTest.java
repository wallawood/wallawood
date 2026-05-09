package io.github.wallawood;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class RequestContextTest {

    @Test
    void addAndGet() {
        var ctx = new RequestContext();
        var uri = URI.create("gemini://localhost/test");
        ctx.add(uri);
        assertEquals(uri, ctx.get(URI.class));
    }

    @Test
    void getReturnsNullWhenMissing() {
        var ctx = new RequestContext();
        assertNull(ctx.get(URI.class));
    }

    @Test
    void addNullIsIgnored() {
        var ctx = new RequestContext();
        ctx.add(null);
        assertTrue(ctx.entrySet().isEmpty());
    }

    @Test
    void addDuplicateTypeThrows() {
        var ctx = new RequestContext();
        ctx.add(Grant.authorized());
        assertThrows(IllegalStateException.class, () -> ctx.add(Grant.none()));
    }

    @Test
    void entrySetIsUnmodifiable() {
        var ctx = new RequestContext();
        ctx.add(URI.create("gemini://localhost"));
        assertThrows(UnsupportedOperationException.class, () -> ctx.entrySet().clear());
    }

    @Test
    void multipleTypesCoexist() {
        var ctx = new RequestContext();
        var uri = URI.create("gemini://localhost");
        var grant = Grant.clearance(5);
        ctx.add(uri);
        ctx.add(grant);
        assertEquals(uri, ctx.get(URI.class));
        assertEquals(grant, ctx.get(Grant.class));
        assertEquals(2, ctx.entrySet().size());
    }

    @Test
    void addWithExplicitKeyStoresUnderThatKey() {
        var ctx = new RequestContext();
        ctx.add(CharSequence.class, "hello");
        assertEquals("hello", ctx.get(CharSequence.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void addWithExplicitKeyRejectsNonAssignable() {
        var ctx = new RequestContext();
        Class raw = String.class;
        assertThrows(IllegalArgumentException.class,
                () -> ctx.add(raw, 42));
    }

    @Test
    void addWithExplicitKeyRejectsDuplicate() {
        var ctx = new RequestContext();
        ctx.add(CharSequence.class, "first");
        assertThrows(IllegalStateException.class,
                () -> ctx.add(CharSequence.class, "second"));
    }

    @Test
    void addWithExplicitKeyNullIsIgnored() {
        var ctx = new RequestContext();
        ctx.add(String.class, null);
        assertTrue(ctx.entrySet().isEmpty());
    }
}
