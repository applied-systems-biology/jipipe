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

    public Point realLocationToGrid(Point location, double zoom) {
        return new Point((int)(location.x / zoom / gridWidth), (int)(location.y / zoom / gridHeight));
    }

    public Point gridToRealLocation(Point gridLocation, double zoom) {
        return new Point((int)(gridLocation.x * zoom * gridWidth), (int)(gridLocation.y * zoom * gridHeight));
    }
}
