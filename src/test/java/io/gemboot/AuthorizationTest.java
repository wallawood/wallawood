package io.gemboot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationTest {

    @Test
    void levelPassesWhenSufficient() {
        assertTrue(Authorization.level(3).check(Grant.at(3)));
        assertTrue(Authorization.level(3).check(Grant.at(5)));
    }

    @Test
    void levelFailsWhenInsufficient() {
        assertFalse(Authorization.level(3).check(Grant.at(2)));
    }

    @Test
    void scopesPassesWhenAllPresent() {
        assertTrue(Authorization.scopes("read").check(Grant.some("read", "write")));
        assertTrue(Authorization.scopes("read", "write").check(Grant.some("read", "write", "admin")));
    }

    @Test
    void scopesFailsWhenMissing() {
        assertFalse(Authorization.scopes("write").check(Grant.some("read")));
        assertFalse(Authorization.scopes("read", "write").check(Grant.some("read")));
    }

    @Test
    void bothLevelAndScopesRequiresBoth() {
        var auth = new Authorization(3, "write");
        assertTrue(auth.check(Grant.all()));
        assertFalse(auth.check(Grant.at(3)));           // level ok, no scopes
        assertFalse(auth.check(Grant.some("write")));   // scopes ok, level -1
    }

    @Test
    void nullGrantFails() {
        assertFalse(Authorization.level(1).check(null));
    }

    @Test
    void noneGrantFails() {
        assertFalse(Authorization.level(1).check(Grant.none()));
    }

    @Test
    void allGrantPassesEverything() {
        assertTrue(Authorization.level(100).check(Grant.all()));
    }

    @Test
    void zeroLevelPassesAnyAuthorized() {
        assertTrue(Authorization.level(0).check(Grant.at(0)));
        assertTrue(Authorization.level(0).check(Grant.at(1)));
    }
}
