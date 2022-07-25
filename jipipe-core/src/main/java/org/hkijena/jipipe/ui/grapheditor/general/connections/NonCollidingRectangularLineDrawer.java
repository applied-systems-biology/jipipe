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

package org.hkijena.jipipe.ui.grapheditor.general.connections;

import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Encapsulates drawing rectangular lines
 */
public class NonCollidingRectangularLineDrawer {

    private final List<RectangularLine> currentSegments = new ArrayList<>();
    private final PriorityQueue<RectangularLine> majorCollisionLines = new PriorityQueue<>(Comparator.comparing(RectangularLine::getA1));
    private final PriorityQueue<RectangularLine> minorCollisionLines = new PriorityQueue<>(Comparator.comparing(RectangularLine::getB1));
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

    public void moveToMajor(int a1, boolean withCollision) {
        RectangularLine lastSegment = getLastSegment();
        RectangularLine newSegment = new RectangularLine(lastSegment.a1, a1, lastSegment.b1, lastSegment.b1);
        if (withCollision) {
            for (RectangularLine collisionLine : majorCollisionLines) {
                int intersection = collisionLine.intersect(newSegment);
                if (intersection > lineSpacer) {
                    addSpacer(lastSegment, newSegment);
                }
            }
        }
        currentSegments.add(newSegment);
    }

    public void moveToMinor(int b, boolean withCollision) {
        RectangularLine lastSegment = getLastSegment();
        RectangularLine newSegment = new RectangularLine(lastSegment.a1, lastSegment.a1, lastSegment.b1, b);
        if (withCollision) {
            for (RectangularLine collisionLine : minorCollisionLines) {
                int intersection = collisionLine.intersect(newSegment);
                if (intersection > lineSpacer) {
                    addSpacer(lastSegment, newSegment);
                }
            }
        }
        currentSegments.add(newSegment);
    }

    public RectangularLine getLastSegment() {
        return currentSegments.get(currentSegments.size() - 1);
    }

    public RectangularLine getFirstSegment() {
        return currentSegments.get(0);
    }

    public void addToMajor(int addA1, boolean withCollision) {
        moveToMajor(getLastSegment().a1 + addA1, withCollision);
    }

    public void addToMinor(int addB, boolean withCollision) {
        moveToMinor(getLastSegment().b1 + addB, withCollision);
    }

    public void drawCurrentSegment(Graphics2D graphics2D, JIPipeGraphViewMode viewMode) {
        Path2D.Float path = new Path2D.Float();
        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            // A = X, B = Y
            path.moveTo(getFirstSegment().a1, getFirstSegment().b1);
            for (int i = 1; i < currentSegments.size(); ++i) {
                RectangularLine currentSegment = currentSegments.get(i);
                minorCollisionLines.add(currentSegment);
                majorCollisionLines.add(currentSegment);
                path.lineTo(currentSegment.a1, currentSegment.b1);
            }
        } else if (viewMode == JIPipeGraphViewMode.Vertical) {
            // A = Y, B = X
            path.moveTo(getFirstSegment().b1, getFirstSegment().a1);
            for (int i = 1; i < currentSegments.size(); ++i) {
                RectangularLine currentSegment = currentSegments.get(i);
                minorCollisionLines.add(currentSegment);
                majorCollisionLines.add(currentSegment);
                path.lineTo(currentSegment.b1, currentSegment.a1);
            }
        }
        graphics2D.draw(path);
    }
}
