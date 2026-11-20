package org.hkijena.jipipe.api.automation.pipeline_map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SegmentStatusLogicTest {

    @Test
    void errorsDominate() {
        assertEquals(SegmentStatus.ERROR, SegmentStatusLogic.compute(1, 0, 5, 5));
        assertEquals(SegmentStatus.ERROR, SegmentStatusLogic.compute(2, 3, 0, 4));
    }

    @Test
    void fullCleanCacheIsOk() {
        assertEquals(SegmentStatus.OK, SegmentStatusLogic.compute(0, 0, 4, 4));
    }

    @Test
    void partialCacheIsWarning() {
        assertEquals(SegmentStatus.WARNING, SegmentStatusLogic.compute(0, 0, 2, 4));
    }

    @Test
    void warningsWithFullCacheAreWarning() {
        assertEquals(SegmentStatus.WARNING, SegmentStatusLogic.compute(0, 1, 4, 4));
    }

    @Test
    void warningsWithoutCacheAreWarning() {
        assertEquals(SegmentStatus.WARNING, SegmentStatusLogic.compute(0, 2, 0, 3));
    }

    @Test
    void nothingCachedIsUnknown() {
        assertEquals(SegmentStatus.UNKNOWN, SegmentStatusLogic.compute(0, 0, 0, 3));
    }

    @Test
    void emptyCompartmentIsUnknown() {
        assertEquals(SegmentStatus.UNKNOWN, SegmentStatusLogic.compute(0, 0, 0, 0));
    }
}
