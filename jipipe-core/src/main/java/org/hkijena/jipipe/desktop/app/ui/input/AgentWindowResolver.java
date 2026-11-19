package org.hkijena.jipipe.desktop.app.ui.input;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure window and point resolution for the agent UI operations (Layer 2, synthetic input).
 * Before any synthetic event is delivered, the target must be inside a showing {@link Window}
 * of the JIPipe JVM; this class decides that from a list of {@link WindowBounds}, so it is
 * testable without a display. The operations adapt live windows via {@link #of(Window)}.
 * Points outside every JIPipe window are refused with the {@link #allowedRectangles(List)}.
 */
public final class AgentWindowResolver {

    private AgentWindowResolver() {
    }

    /**
     * The two facts about a window the resolver needs: its on-screen bounds and whether it is
     * currently showing. A record (instead of {@link Window}) keeps the helper headless-testable.
     * Bounds are in screen coordinates and include the window decoration.
     */
    public record WindowBounds(Rectangle onScreenBounds, boolean showing) {
    }

    /**
     * Resolves the topmost showing candidate whose on-screen bounds contain the point.
     * The list is ordered like {@link Window#getWindows()}: the topmost window is LAST,
     * so the search walks the list in reverse and returns the first containment hit.
     * Returns null when no showing candidate contains the point.
     *
     * @param bounds     the window candidates, topmost last
     * @param screenPoint the point in screen coordinates
     * @return the resolved bounds or null
     */
    public static WindowBounds resolve(List<WindowBounds> bounds, Point screenPoint) {
        for (int i = bounds.size() - 1; i >= 0; i--) {
            WindowBounds candidate = bounds.get(i);
            if (candidate.showing() && isInside(candidate, screenPoint)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * The on-screen bounds of every showing candidate, in list order. This is the area a
     * synthetic-input operation accepts; it is returned in refusal responses so an agent
     * can correct an out-of-window point.
     */
    public static List<Rectangle> allowedRectangles(List<WindowBounds> bounds) {
        List<Rectangle> rectangles = new ArrayList<>();
        for (WindowBounds candidate : bounds) {
            if (candidate.showing()) {
                rectangles.add(candidate.onScreenBounds());
            }
        }
        return rectangles;
    }

    /**
     * Whether the point is inside the candidate's on-screen bounds. Window bounds enclose the
     * whole window, so a point over the window background (outside any component) is inside.
     */
    public static boolean isInside(WindowBounds bounds, Point screenPoint) {
        return bounds.onScreenBounds().contains(screenPoint);
    }

    /**
     * Adapts a live window to the resolver's record. Call on the EDT like other window queries.
     */
    public static WindowBounds of(Window window) {
        return new WindowBounds(window.getBounds(), window.isShowing());
    }
}
