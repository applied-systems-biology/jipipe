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

package org.hkijena.jipipe.ui.grapheditor;

import java.awt.*;

/**
 * The direction how a canvas renders the nodes
 */
public enum JIPipeGraphViewMode {
    @Deprecated
    Horizontal(25, 50),
    @Deprecated
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


    @Override
    public String toString() {
        if (this == JIPipeGraphViewMode.VerticalCompact) {
            return "Vertical (compact)";
        }
        return name();
    }
}
