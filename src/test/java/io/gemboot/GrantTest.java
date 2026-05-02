package io.gemboot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrantTest {

    @Test
    void noneIsNotAuthorized() {
        Grant grant = Grant.none();
        assertFalse(grant.isAuthorized());
        assertEquals(-1, grant.level());
        assertTrue(grant.scopes().isEmpty());
    }

    @Test
    void allIsAuthorizedWithMaxLevel() {
        Grant grant = Grant.all();
        assertTrue(grant.isAuthorized());
        assertEquals(Integer.MAX_VALUE, grant.level());
        assertTrue(grant.scopes().isEmpty());
    }

    @Test
    void atSetsLevel() {
        Grant grant = Grant.at(3);
        assertTrue(grant.isAuthorized());
        assertEquals(3, grant.level());
        assertTrue(grant.scopes().isEmpty());
    }

    @Test
    void someSetsScopes() {
        Grant grant = Grant.some("read", "write");
        assertTrue(grant.isAuthorized());
        assertEquals(-1, grant.level());
        assertEquals(2, grant.scopes().size());
        assertTrue(grant.scopes().contains("read"));
        assertTrue(grant.scopes().contains("write"));
    }

    @Test
    void hasScopesReturnsTrueWhenAllPresent() {
        Grant grant = Grant.some("read", "write", "admin");
        assertTrue(grant.hasScopes("read", "write"));
    }

    @Test
    void hasScopesReturnsFalseWhenMissing() {
        Grant grant = Grant.some("read");
        assertFalse(grant.hasScopes("read", "write"));
    }

    @Test
    void scopesAreUnmodifiable() {
        Grant grant = Grant.some("read");
        assertThrows(UnsupportedOperationException.class, () -> grant.scopes().add("write"));
    }
}
