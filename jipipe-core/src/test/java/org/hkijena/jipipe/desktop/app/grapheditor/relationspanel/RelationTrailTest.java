/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.grapheditor.relationspanel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RelationTrail}, the visitation trail behind the Relations
 * panel (MR 3 of the navigator overhaul, #1358).
 */
class RelationTrailTest {

    @Test
    void freshTrailIsEmpty() {
        RelationTrail trail = new RelationTrail(6);
        assertNull(trail.current());
        assertEquals(0, trail.stepCount());
        assertFalse(trail.canGoBack());
        assertFalse(trail.canGoForward());
        assertNull(trail.goBack());
        assertNull(trail.goForward());
    }

    @Test
    void navigateToAppendsAndClearsForward() {
        RelationTrail trail = new RelationTrail(6);
        trail.navigateTo("a");
        trail.navigateTo("b");
        trail.navigateTo("c");
        assertEquals(3, trail.stepCount());
        assertEquals("c", trail.current());
        assertTrue(trail.canGoBack());
        assertFalse(trail.canGoForward());
        assertEquals("b", trail.goBack());
        assertTrue(trail.canGoForward());
        trail.navigateTo("d");
        assertEquals("d", trail.current());
        assertFalse(trail.canGoForward());
        assertEquals(3, trail.stepCount());
        assertEquals("b", trail.goBack());
    }

    @Test
    void backAndForwardWalkTheVisitedSequenceAndStopAtTheEnds() {
        RelationTrail trail = new RelationTrail(6);
        trail.navigateTo("a");
        trail.navigateTo("b");
        trail.navigateTo("c");
        assertEquals("b", trail.goBack());
        assertEquals("a", trail.goBack());
        assertFalse(trail.canGoBack());
        assertNull(trail.goBack());
        assertEquals("a", trail.current());
        assertEquals("b", trail.goForward());
        assertEquals("c", trail.goForward());
        assertFalse(trail.canGoForward());
        assertNull(trail.goForward());
        assertEquals("c", trail.current());
    }

    @Test
    void capDropsTheOldestSoBacksteppingBeyondTheCapIsImpossible() {
        RelationTrail trail = new RelationTrail(3);
        trail.navigateTo("a");
        trail.navigateTo("b");
        trail.navigateTo("c");
        trail.navigateTo("d");
        assertEquals(3, trail.stepCount());
        assertEquals("d", trail.current());
        assertEquals("c", trail.goBack());
        assertEquals("b", trail.goBack());
        assertFalse(trail.canGoBack());
        assertNull(trail.goBack());
        assertEquals("c", trail.goForward());
    }

    @Test
    void opacityRankIsZeroForTheCurrentAndRampsOverTheFiveStepsBack() {
        RelationTrail trail = new RelationTrail(10);
        trail.navigateTo("n1");
        trail.navigateTo("n2");
        trail.navigateTo("n3");
        trail.navigateTo("n4");
        trail.navigateTo("n5");
        trail.navigateTo("n6");
        trail.navigateTo("n7");
        assertEquals(0, trail.opacityRank("n7"));
        assertEquals(1, trail.opacityRank("n6"));
        assertEquals(2, trail.opacityRank("n5"));
        assertEquals(3, trail.opacityRank("n4"));
        assertEquals(4, trail.opacityRank("n3"));
        assertEquals(5, trail.opacityRank("n2"));
        assertEquals(-1, trail.opacityRank("n1"));
        assertEquals(-1, trail.opacityRank("unrelated"));
    }

    /**
     * History a, b, a, c with current c: walking backward, the nearest
     * occurrence of a ranks 1 and b ranks 2; the older a occurrence adds no
     * second rank.
     */
    @Test
    void duplicateVisitsKeepTheRankOfTheNearestOccurrence() {
        RelationTrail trail = new RelationTrail(10);
        trail.navigateTo("a");
        trail.navigateTo("b");
        trail.navigateTo("a");
        trail.navigateTo("c");
        assertEquals(0, trail.opacityRank("c"));
        assertEquals(1, trail.opacityRank("a"));
        assertEquals(2, trail.opacityRank("b"));
    }

    /**
     * After going back, the node ahead (forward history) is not on the back
     * ramp: only the current node and the steps before it are ranked.
     */
    @Test
    void forwardHistoryIsNotRankedAfterGoingBack() {
        RelationTrail trail = new RelationTrail(6);
        trail.navigateTo("a");
        trail.navigateTo("b");
        trail.navigateTo("c");
        assertEquals("b", trail.goBack());
        assertEquals(0, trail.opacityRank("b"));
        assertEquals(1, trail.opacityRank("a"));
        assertEquals(-1, trail.opacityRank("c"));
    }
}
