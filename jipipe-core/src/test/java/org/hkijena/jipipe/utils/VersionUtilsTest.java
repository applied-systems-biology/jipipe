package org.hkijena.jipipe.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionUtilsTest {

    @Test
    public void testIsWithinVersionRangeInclusive() {
        assertTrue(VersionUtils.isWithinVersionRangeInclusive("5.3.0", "5.0.0", "6.0.0"));
        assertTrue(VersionUtils.isWithinVersionRangeInclusive("5.0.0", "5.0.0", "6.0.0"));
        assertTrue(VersionUtils.isWithinVersionRangeInclusive("6.0.0", "5.0.0", "6.0.0"));
        assertFalse(VersionUtils.isWithinVersionRangeInclusive("4.999.0", "5.0.0", "6.0.0"));
    }

    @Test
    public void testIsWithinVersionRangeExclusive() {
        assertTrue(VersionUtils.isWithinVersionRangeExclusive("5.3.0", "5.0.0", "6.0.0"));
        assertFalse(VersionUtils.isWithinVersionRangeExclusive("5.0.0", "5.0.0", "6.0.0"));
        assertFalse(VersionUtils.isWithinVersionRangeExclusive("6.0.0", "5.0.0", "6.0.0"));
        assertFalse(VersionUtils.isWithinVersionRangeExclusive("4.999.0", "5.0.0", "6.0.0"));
    }

    @Test
    public void testIsNewerThan() {
        assertTrue(VersionUtils.isNewerThan("6.0.0", "5.0.0"));
        assertFalse(VersionUtils.isNewerThan("4.999.0", "5.0.0"));
    }

    @Test
    public void testIsNewerThanOrEqual() {
        assertTrue(VersionUtils.isNewerThanOrEqual("6.0.0", "5.0.0"));
        assertTrue(VersionUtils.isNewerThanOrEqual("6.0.0", "6.0.0"));
        assertFalse(VersionUtils.isNewerThanOrEqual("4.999.0", "5.0.0"));
    }

    @Test
    public void testIsOlderThan() {
        assertFalse(VersionUtils.isOlderThan("6.0.0", "5.0.0"));
        assertTrue(VersionUtils.isOlderThan("4.999.0", "5.0.0"));
    }

    @Test
    public void testIsOlderThanOrEqual() {
        assertFalse(VersionUtils.isOlderThanOrEqual("6.0.0", "5.0.0"));
        assertTrue(VersionUtils.isOlderThanOrEqual("6.0.0", "6.0.0"));
        assertTrue(VersionUtils.isOlderThanOrEqual("4.999.0", "5.0.0"));
    }
}
