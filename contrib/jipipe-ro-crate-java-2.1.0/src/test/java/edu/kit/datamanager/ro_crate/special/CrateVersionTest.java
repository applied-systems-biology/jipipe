package edu.kit.datamanager.ro_crate.special;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class CrateVersionTest {

    @Test
    public void testIsGreaterThan() {
        assertTrue(CrateVersion.V1P2_DRAFT.isGreaterThan(CrateVersion.V1P1));
        assertFalse(CrateVersion.V1P1.isGreaterThan(CrateVersion.V1P2_DRAFT));
    }

    @Test
    public void testIsGreaterOrEqualThan() {
        assertTrue(CrateVersion.LATEST_UNSTABLE.isGreaterOrEqualThan(CrateVersion.LATEST_STABLE));
        assertTrue(CrateVersion.V1P2_DRAFT.isGreaterOrEqualThan(CrateVersion.V1P1));
        assertFalse(CrateVersion.V1P1.isGreaterOrEqualThan(CrateVersion.V1P2_DRAFT));
    }

    @Test
    public void testIsLowerThan() {
        assertTrue(CrateVersion.V1P1.isLowerThan(CrateVersion.V1P2_DRAFT));
        assertFalse(CrateVersion.V1P2_DRAFT.isLowerThan(CrateVersion.V1P1));
    }

    @Test
    public void testIsLowerOrEqualThan() {
        assertTrue(CrateVersion.LATEST_STABLE.isLowerOrEqualThan(CrateVersion.LATEST_UNSTABLE));
        assertTrue(CrateVersion.V1P1.isLowerOrEqualThan(CrateVersion.V1P2_DRAFT));
        assertFalse(CrateVersion.V1P2_DRAFT.isLowerOrEqualThan(CrateVersion.V1P1));
    }

    @Test
    void testFromSpecUri() {
        // fromSpecUri has to work with the internally stored values
        Arrays.stream(CrateVersion.values())
                .forEach(version -> assertTrue(CrateVersion.fromSpecUri(version.conformsTo).isPresent()));
        // but it has to fail for other values
        assertTrue(CrateVersion.fromSpecUri("https://example.com/my/profile/1.1").isEmpty());
    }

    @Test
    void testGetConformsToUri() {
        // getConformsToUri assumes internally to never throw, and therefore to never
        // return null.
        Arrays.stream(CrateVersion.values())
                .forEach(version -> assertNotNull(version));
    }

    @Test
    void testIsStable() {
        assertTrue(CrateVersion.LATEST_STABLE.isStable());
        assertTrue(CrateVersion.V1P1.isStable());

        assertFalse(CrateVersion.LATEST_UNSTABLE.isStable());
        assertFalse(CrateVersion.V1P2_DRAFT.isStable());
    }
}
