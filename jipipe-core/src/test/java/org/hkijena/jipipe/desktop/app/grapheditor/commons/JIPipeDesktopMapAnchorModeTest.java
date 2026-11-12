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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopMapState.AnchorMode;
import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the geometry of the floating map's anchor model: detection of the
 * nearest snapping point, placement into the overlay, and offset capture.
 * Pure geometry against a fixed overlay ({@code 1000 x 800}) and map size
 * ({@code 280 x 200}); the panel wiring is not covered here.
 */
class JIPipeDesktopMapAnchorModeTest {

    private static final int OVERLAY_WIDTH = 1000;
    private static final int OVERLAY_HEIGHT = 800;
    private static final int MAP_WIDTH = 280;
    private static final int MAP_HEIGHT = 200;

    @Test
    void detectPicksTheNearestCorner() {
        // 8 px from the right edge, 16 px from the top
        assertEquals(AnchorMode.TopRight, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 712, 16));
        // 16 px from the left edge, 14 px from the bottom
        assertEquals(AnchorMode.BottomLeft, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 586));
    }

    @Test
    void detectPicksTheNearestEdge() {
        // 20 px from the left, vertically centered
        assertEquals(AnchorMode.CenterLeft, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 20, 300));
        // horizontally centered, 10 px from the top
        assertEquals(AnchorMode.TopCenter, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 360, 10));
    }

    @Test
    void detectTreatsAnAxisAsCenteredOnlyWithinTheTolerance() {
        // 16 px difference between the horizontal edge distances: centered
        assertEquals(AnchorMode.TopCenter, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 352, 10));
        // 60 px difference: the left side wins
        assertEquals(AnchorMode.TopLeft, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 330, 10));
    }

    @Test
    void detectCentersBothAxesForAMapInTheMiddle() {
        assertEquals(AnchorMode.CenterCenter, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT,
                (OVERLAY_WIDTH - MAP_WIDTH) / 2, (OVERLAY_HEIGHT - MAP_HEIGHT) / 2));
    }

    @Test
    void detectNeverReturnsAuto() {
        assertNotEquals(AnchorMode.Auto, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 0, 0));
        assertNotEquals(AnchorMode.Auto, AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 400, 300));
    }

    @Test
    void placePositionsAllNineAnchorsAtTheirOffset() {
        // Centered axes ignore the offset and align to the exact center
        assertEquals(new Point(16, 16), AnchorMode.TopLeft.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
        assertEquals(new Point((OVERLAY_WIDTH - MAP_WIDTH) / 2, 16), AnchorMode.TopCenter.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
        assertEquals(new Point(OVERLAY_WIDTH - MAP_WIDTH - 16, 16), AnchorMode.TopRight.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
        assertEquals(new Point(16, (OVERLAY_HEIGHT - MAP_HEIGHT) / 2), AnchorMode.CenterLeft.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
        assertEquals(new Point((OVERLAY_WIDTH - MAP_WIDTH) / 2, (OVERLAY_HEIGHT - MAP_HEIGHT) / 2), AnchorMode.CenterCenter.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
        assertEquals(new Point(OVERLAY_WIDTH - MAP_WIDTH - 16, (OVERLAY_HEIGHT - MAP_HEIGHT) / 2), AnchorMode.CenterRight.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
        assertEquals(new Point(16, OVERLAY_HEIGHT - MAP_HEIGHT - 16), AnchorMode.BottomLeft.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
        assertEquals(new Point((OVERLAY_WIDTH - MAP_WIDTH) / 2, OVERLAY_HEIGHT - MAP_HEIGHT - 16), AnchorMode.BottomCenter.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
        assertEquals(new Point(OVERLAY_WIDTH - MAP_WIDTH - 16, OVERLAY_HEIGHT - MAP_HEIGHT - 16), AnchorMode.BottomRight.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 16, 16));
    }

    @Test
    void placeReproducesThePositionForCornerAnchors() {
        // The offset captured from a position round-trips it exactly
        Point position = new Point(712, 16);
        AnchorMode anchor = AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, position.x, position.y);
        Point offset = anchor.captureOffset(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, position.x, position.y);
        assertEquals(position, anchor.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, offset.x, offset.y));
    }

    @Test
    void placeNormalizesCenteredAxesToTheExactCenter() {
        // Detected as TopCenter from an off-center x: placement centers x exactly
        AnchorMode anchor = AnchorMode.detect(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 352, 10);
        assertEquals(AnchorMode.TopCenter, anchor);
        Point offset = anchor.captureOffset(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 352, 10);
        assertEquals(new Point((OVERLAY_WIDTH - MAP_WIDTH) / 2, 10), anchor.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, offset.x, offset.y));
    }

    @Test
    void captureOffsetMeasuresFromTheAnchoredSides() {
        // TopRight: distance to the right edge and to the top
        Point topRight = AnchorMode.TopRight.captureOffset(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 712, 40);
        assertEquals(8, topRight.x);
        assertEquals(40, topRight.y);
        // BottomLeft: distance to the left edge and to the bottom
        Point bottomLeft = AnchorMode.BottomLeft.captureOffset(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 20, 586);
        assertEquals(20, bottomLeft.x);
        assertEquals(14, bottomLeft.y);
    }

    @Test
    void autoCannotPlaceOrCapture() {
        assertThrows(UnsupportedOperationException.class, () -> AnchorMode.Auto.place(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 0, 0));
        assertThrows(UnsupportedOperationException.class, () -> AnchorMode.Auto.captureOffset(OVERLAY_WIDTH, OVERLAY_HEIGHT, MAP_WIDTH, MAP_HEIGHT, 0, 0));
    }
}
