package io.gemboot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationTest {

    @Test
    void requireClearancePassesWhenSufficient() {
        assertTrue(Authorization.requireClearance(3).check(Grant.clearance(3)));
        assertTrue(Authorization.requireClearance(3).check(Grant.clearance(5)));
    }

    @Test
    void requireClearanceFailsWhenInsufficient() {
        assertFalse(Authorization.requireClearance(3).check(Grant.clearance(2)));
    }

    @Test
    void requireScopesPassesWhenAllPresent() {
        assertTrue(Authorization.requireScopes("read").check(Grant.scopes("read", "write")));
        assertTrue(Authorization.requireScopes("read", "write").check(Grant.scopes("read", "write", "admin")));
    }

    @Test
    void requireScopesFailsWhenMissing() {
        assertFalse(Authorization.requireScopes("write").check(Grant.scopes("read")));
        assertFalse(Authorization.requireScopes("read", "write").check(Grant.scopes("read")));
    }

    @Test
    void requireAuthorizedPassesWhenAuthorized() {
        assertTrue(Authorization.requireAuthorized().check(Grant.authorized()));
    }

    @Test
    void requireAuthorizedFailsWhenNotAuthorized() {
        assertFalse(Authorization.requireAuthorized().check(Grant.clearance(5)));
        assertFalse(Authorization.requireAuthorized().check(Grant.scopes("admin")));
        assertFalse(Authorization.requireAuthorized().check(Grant.none()));
    }

    @Test
    void compositeRequiresBothClearanceAndScopes() {
        var auth = Authorization.builder()
                .requireClearance(3)
                .requireScopes("write")
                .build();
        var full = Grant.builder().clearance(3).addScope("write").build();
        assertTrue(auth.check(full));
        assertFalse(auth.check(Grant.clearance(3)));
        assertFalse(auth.check(Grant.scopes("write")));
    }

    @Test
    void compositeAllThreeDimensions() {
        var auth = Authorization.builder()
                .requireAuthorized()
                .requireClearance(2)
                .requireScopes("admin")
                .build();
        var full = Grant.builder().authorized(true).clearance(2).addScope("admin").build();
        assertTrue(auth.check(full));
        assertFalse(auth.check(Grant.builder().clearance(2).addScope("admin").build()));
        assertFalse(auth.check(Grant.builder().authorized(true).addScope("admin").build()));
        assertFalse(auth.check(Grant.builder().authorized(true).clearance(2).build()));
    }

    @Test
    void nullGrantFails() {
        assertFalse(Authorization.requireClearance(1).check(null));
        assertFalse(Authorization.requireAuthorized().check(null));
    }

    @Test
    void noneGrantFailsAll() {
        assertFalse(Authorization.requireClearance(0).check(Grant.none()));
        assertFalse(Authorization.requireAuthorized().check(Grant.none()));
    }

    @Test
    void zeroClearancePassesZeroLevel() {
        assertTrue(Authorization.requireClearance(0).check(Grant.clearance(0)));
        assertTrue(Authorization.requireClearance(0).check(Grant.clearance(1)));
    }

    @Test
    void dimensionsAreOrthogonal() {
        assertFalse(Authorization.requireAuthorized().check(Grant.clearance(100)));
        assertFalse(Authorization.requireClearance(1).check(Grant.authorized()));
        assertFalse(Authorization.requireScopes("x").check(Grant.authorized()));
    }
}
