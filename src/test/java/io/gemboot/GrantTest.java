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
    void authorizedOnlySetsAuthorizedFlag() {
        Grant grant = Grant.authorized();
        assertTrue(grant.isAuthorized());
        assertEquals(-1, grant.level());
        assertTrue(grant.scopes().isEmpty());
    }

    @Test
    void clearanceOnlySetsLevel() {
        Grant grant = Grant.clearance(3);
        assertFalse(grant.isAuthorized());
        assertEquals(3, grant.level());
        assertTrue(grant.scopes().isEmpty());
    }

    @Test
    void scopesOnlySetsScopes() {
        Grant grant = Grant.scopes("read", "write");
        assertFalse(grant.isAuthorized());
        assertEquals(-1, grant.level());
        assertEquals(2, grant.scopes().size());
        assertTrue(grant.scopes().contains("read"));
        assertTrue(grant.scopes().contains("write"));
    }

    @Test
    void hasScopesReturnsTrueWhenAllPresent() {
        Grant grant = Grant.scopes("read", "write", "admin");
        assertTrue(grant.hasScopes("read", "write"));
    }

    @Test
    void hasScopesReturnsFalseWhenMissing() {
        Grant grant = Grant.scopes("read");
        assertFalse(grant.hasScopes("read", "write"));
    }

    @Test
    void scopesAreUnmodifiable() {
        Grant grant = Grant.scopes("read");
        assertThrows(UnsupportedOperationException.class, () -> grant.scopes().add("write"));
    }

    @Test
    void builderCombinesAllDimensions() {
        Grant grant = Grant.builder()
                .authorized(true)
                .clearance(5)
                .addScope("admin")
                .build();
        assertTrue(grant.isAuthorized());
        assertEquals(5, grant.level());
        assertTrue(grant.hasScopes("admin"));
    }
}
