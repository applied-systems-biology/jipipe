package org.hkijena.jipipe.ui.grapheditor;

import java.awt.*;

/**
 * The direction how a canvas renders the nodes
 */
public enum JIPipeGraphViewMode {
    Horizontal(25, 50),
    Vertical(25, 50);
    private final int gridWidth;
    private final int gridHeight;

    JIPipeGraphViewMode(int gridWidth, int gridHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public Point getGridPoint(Point point) {
        return new Point(point.x * gridWidth, point.y * gridHeight);
    }

    public Point getNextGridPoint(Point point) {
        int y = (int) Math.rint(point.y * 1.0 / gridHeight) * gridHeight;
        int x = (int) Math.rint(point.x * 1.0 / gridWidth) * gridWidth;
        return new Point(x, y);
    }
}
