package org.hkijena.jipipe.plugins.cellpose.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CellposeVersionUtilsTest {

    @Test
    public void testRequiresCellpose42() {
        assertTrue(CellposeVersionUtils.requiresCellpose42("cpsam_v2"));
        assertTrue(CellposeVersionUtils.requiresCellpose42("cpdino"));
        assertTrue(CellposeVersionUtils.requiresCellpose42("cpdino-vitb"));
        assertFalse(CellposeVersionUtils.requiresCellpose42("cpsam"));
        assertFalse(CellposeVersionUtils.requiresCellpose42(null));
    }

    @Test
    public void testIsModelSupportedOldVersion() {
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam", "4.0.7"));
        assertTrue(CellposeVersionUtils.isModelSupported(null, "4.0.7"));
        assertFalse(CellposeVersionUtils.isModelSupported("cpsam_v2", "4.0.7"));
        assertFalse(CellposeVersionUtils.isModelSupported("cpdino", "4.0.7"));
        assertFalse(CellposeVersionUtils.isModelSupported("cpdino-vitb", "4.0.7"));
    }

    @Test
    public void testIsModelSupportedNewVersion() {
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam", "4.2.1"));
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam_v2", "4.2.1"));
        assertTrue(CellposeVersionUtils.isModelSupported("cpdino", "4.2.1"));
        assertTrue(CellposeVersionUtils.isModelSupported("cpdino-vitb", "4.2.1"));
        assertTrue(CellposeVersionUtils.isModelSupported(null, "4.2.1"));
    }

    @Test
    public void testIsModelSupportedNullVersion() {
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam", null));
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam_v2", null));
        assertTrue(CellposeVersionUtils.isModelSupported(null, null));
    }
}
