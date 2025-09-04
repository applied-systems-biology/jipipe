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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import java.awt.*;

/**
 * Contains methods and fields that are used for converting grid space from/to real space
 */
public class JIPipeDesktopGraphCanvasGrid {
    public static final int GRID_WIDTH = 25;
    public static final int GRID_HEIGHT = 25;

    private  JIPipeDesktopGraphCanvasGrid() {

    }

    public static Point realLocationToGrid(Point location, double zoom) {
        return new Point((int) Math.round(location.x / zoom / GRID_WIDTH), (int) Math.round(location.y / zoom / GRID_HEIGHT));
    }

    public static Point gridToRealLocation(Point gridLocation, double zoom) {
        return new Point((int) (gridLocation.x * zoom * GRID_WIDTH), (int) (gridLocation.y * zoom * GRID_HEIGHT));
    }

    public static Dimension gridToRealSize(Dimension gridSize, double zoom) {
        return new Dimension((int) Math.round(gridSize.width * GRID_WIDTH * zoom), (int) Math.round(gridSize.height * GRID_HEIGHT * zoom));
    }
}
