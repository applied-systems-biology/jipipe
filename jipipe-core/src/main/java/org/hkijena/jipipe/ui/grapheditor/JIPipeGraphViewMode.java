package org.hkijena.jipipe.ui.grapheditor;

import java.awt.Dimension;
import java.awt.Point;

/**
 * The direction how a canvas renders the nodes
 */
public enum JIPipeGraphViewMode {
    Horizontal(25, 50),
    Vertical(25, 50),
    VerticalCompact(25, 25);
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
        return new Point((int) Math.round(location.x / zoom / gridWidth), (int) Math.round(location.y / zoom / gridHeight));
    }

    public Point gridToRealLocation(Point gridLocation, double zoom) {
        return new Point((int) (gridLocation.x * zoom * gridWidth), (int) (gridLocation.y * zoom * gridHeight));
    }

    public Dimension gridToRealSize(Dimension gridSize, double zoom) {
        return new Dimension((int) Math.round(gridSize.width * gridWidth * zoom), (int) Math.round(gridSize.height * gridHeight * zoom));
    }
}
