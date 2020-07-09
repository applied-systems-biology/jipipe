/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.connections;

import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates drawing rectangular lines
 */
public class RectangularLineDrawer {

    private final List<RectangularLine> currentSegments = new ArrayList<>();
    private int lineSpacer = 4;

    public void start(int a0, int b) {
        currentSegments.clear();
        currentSegments.add(new RectangularLine(0, a0, 0, b));
    }

    private void addSpacer(RectangularLine lastSegment, RectangularLine newSegment) {
        if (lastSegment.a1 == newSegment.a1) {
            lastSegment.a1 += lineSpacer;
            newSegment.a1 += lineSpacer;
            newSegment.a0 += lineSpacer;
        }
        if (lastSegment.b1 == newSegment.b1) {
            lastSegment.b1 += lineSpacer;
            newSegment.b1 += lineSpacer;
        }
    }

    public void moveToMajor(int a1) {
        RectangularLine lastSegment = getLastSegment();
        RectangularLine newSegment = new RectangularLine(lastSegment.a1, a1, lastSegment.b1, lastSegment.b1);
        currentSegments.add(newSegment);
    }

    public void moveToMinor(int b) {
        RectangularLine lastSegment = getLastSegment();
        RectangularLine newSegment = new RectangularLine(lastSegment.a1, lastSegment.a1, lastSegment.b1, b);
        currentSegments.add(newSegment);
    }

    public RectangularLine getLastSegment() {
        return currentSegments.get(currentSegments.size() - 1);
    }

    public RectangularLine getFirstSegment() {
        return currentSegments.get(0);
    }

    public void addToMajor(int addA1) {
        moveToMajor(getLastSegment().a1 + addA1);
    }

    public void addToMinor(int addB) {
        moveToMinor(getLastSegment().b1 + addB);
    }

    public void drawCurrentSegment(Graphics2D graphics2D, JIPipeGraphCanvasUI.ViewMode viewMode) {
        Path2D.Float path = new Path2D.Float();
        if (viewMode == JIPipeGraphCanvasUI.ViewMode.Horizontal) {
            // A = X, B = Y
            path.moveTo(getFirstSegment().a1, getFirstSegment().b1);
            for (int i = 1; i < currentSegments.size(); ++i) {
                RectangularLine currentSegment = currentSegments.get(i);
                path.lineTo(currentSegment.a1, currentSegment.b1);
            }
        } else if (viewMode == JIPipeGraphCanvasUI.ViewMode.Vertical) {
            // A = Y, B = X
            path.moveTo(getFirstSegment().b1, getFirstSegment().a1);
            for (int i = 1; i < currentSegments.size(); ++i) {
                RectangularLine currentSegment = currentSegments.get(i);
                path.lineTo(currentSegment.b1, currentSegment.a1);
            }
        }
        graphics2D.draw(path);
    }
}
