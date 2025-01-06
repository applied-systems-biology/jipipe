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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;

import java.awt.*;

public class TurboRegPointHandler extends Roi {

/*....................................................................
	Public variables
....................................................................*/

    /**
     * The magnifying tool is set in eleventh position to be coherent with
     * ImageJ.
     **/
    public static final int MAGNIFIER = 11;

    /**
     * The moving tool is set in second position to be coherent with the
     * <code>PointPicker_</code> plugin.
     **/
    public static final int MOVE_CROSS = 1;

    /**
     * The number of points we are willing to deal with is at most
     * <code>4</code>.
     **/
    public static final int NUM_POINTS = 4;

/*....................................................................
	Private variables
....................................................................*/

    /**
     * The drawn landmarks fit in a 11x11 matrix.
     **/
    private static final int CROSS_HALFSIZE = 5;

    /**
     * The golden ratio mathematical constant determines where to put the
     * initial landmarks.
     **/
    private static final double GOLDEN_RATIO = 0.5 * (Math.sqrt(5.0) - 1.0);

    private final Point[] point = new Point[NUM_POINTS];
    private final Color[] spectrum = new Color[NUM_POINTS];
    private double[][] precisionPoint = new double[NUM_POINTS][2];
    private TurboRegTransformation transformation;
    private int currentPoint = 0;
    private boolean interactive = true;
    private boolean started = false;

/*....................................................................
	Public methods
....................................................................*/

    /**
     * Draw the landmarks. Outline the current point if the window has focus.
     *
     * @param g Graphics environment.
     **/
    public void draw(
            final Graphics g
    ) {
        if (started) {
            final double mag = ic.getMagnification();
            final int dx = (int) (mag / 2.0);
            final int dy = (int) (mag / 2.0);
            Point p;
            if (transformation == TurboRegTransformation.RigidBody) {
                if (currentPoint == 0) {
                    for (int k = 1; (k < transformation.getNativeValue()); k++) {
                        p = point[k];
                        g.setColor(spectrum[k]);
                        g.fillRect(ic.screenX(p.x) - 2 + dx,
                                ic.screenY(p.y) - 2 + dy, 5, 5);
                    }
                    drawHorizon(g);
                    p = point[0];
                    g.setColor(spectrum[0]);
                    if (WindowManager.getCurrentImage() == imp) {
                        g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                                ic.screenY(p.y - 1) + dy,
                                ic.screenX(p.x - 1) + dx,
                                ic.screenY(p.y - 1) + dy);
                        g.drawLine(ic.screenX(p.x - 1) + dx,
                                ic.screenY(p.y - 1) + dy,
                                ic.screenX(p.x - 1) + dx,
                                ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
                        g.drawLine(ic.screenX(p.x - 1) + dx,
                                ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
                                ic.screenX(p.x + 1) + dx,
                                ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
                        g.drawLine(ic.screenX(p.x + 1) + dx,
                                ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
                                ic.screenX(p.x + 1) + dx,
                                ic.screenY(p.y - 1) + dy);
                        g.drawLine(ic.screenX(p.x + 1) + dx,
                                ic.screenY(p.y - 1) + dy,
                                ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                                ic.screenY(p.y - 1) + dy);
                        g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                                ic.screenY(p.y - 1) + dy,
                                ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                                ic.screenY(p.y + 1) + dy);
                        g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                                ic.screenY(p.y + 1) + dy,
                                ic.screenX(p.x + 1) + dx,
                                ic.screenY(p.y + 1) + dy);
                        g.drawLine(ic.screenX(p.x + 1) + dx,
                                ic.screenY(p.y + 1) + dy,
                                ic.screenX(p.x + 1) + dx,
                                ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
                        g.drawLine(ic.screenX(p.x + 1) + dx,
                                ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
                                ic.screenX(p.x - 1) + dx,
                                ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
                        g.drawLine(ic.screenX(p.x - 1) + dx,
                                ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
                                ic.screenX(p.x - 1) + dx,
                                ic.screenY(p.y + 1) + dy);
                        g.drawLine(ic.screenX(p.x - 1) + dx,
                                ic.screenY(p.y + 1) + dy,
                                ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                                ic.screenY(p.y + 1) + dy);
                        g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                                ic.screenY(p.y + 1) + dy,
                                ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                                ic.screenY(p.y - 1) + dy);
                        if (1.0 < ic.getMagnification()) {
                            g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
                                    ic.screenY(p.y) + dy,
                                    ic.screenX(p.x + CROSS_HALFSIZE) + dx,
                                    ic.screenY(p.y) + dy);
                            g.drawLine(ic.screenX(p.x) + dx,
                                    ic.screenY(p.y - CROSS_HALFSIZE) + dy,
                                    ic.screenX(p.x) + dx,
                                    ic.screenY(p.y + CROSS_HALFSIZE) + dy);
                        }
                    } else {
                        g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
                                ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
                                ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
                                ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
                        g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
                                ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
                                ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
                                ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
                    }
                } else {
                    p = point[0];
                    g.setColor(spectrum[0]);
                    g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
                            ic.screenY(p.y) + dy,
                            ic.screenX(p.x + CROSS_HALFSIZE) + dx,
                            ic.screenY(p.y) + dy);
                    g.drawLine(ic.screenX(p.x) + dx,
                            ic.screenY(p.y - CROSS_HALFSIZE) + dy,
                            ic.screenX(p.x) + dx,
                            ic.screenY(p.y + CROSS_HALFSIZE) + dy);
                    drawHorizon(g);
                    if (WindowManager.getCurrentImage() == imp) {
                        drawArcs(g);
                        for (int k = 1; (k < transformation.getNativeValue()); k++) {
                            p = point[k];
                            g.setColor(spectrum[k]);
                            if (k == currentPoint) {
                                g.drawRect(ic.screenX(p.x) - 3 + dx,
                                        ic.screenY(p.y) - 3 + dy, 6, 6);
                            } else {
                                g.fillRect(ic.screenX(p.x) - 2 + dx,
                                        ic.screenY(p.y) - 2 + dy, 5, 5);
                            }
                        }
                    } else {
                        for (int k = 1; (k < transformation.getNativeValue()); k++) {
                            p = point[k];
                            g.setColor(spectrum[k]);
                            if (k == currentPoint) {
                                g.drawRect(ic.screenX(p.x) - 2 + dx,
                                        ic.screenY(p.y) - 2 + dy, 5, 5);
                            } else {
                                g.fillRect(ic.screenX(p.x) - 2 + dx,
                                        ic.screenY(p.y) - 2 + dy, 5, 5);
                            }
                        }
                    }
                }
            } else {
                for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                    p = point[k];
                    g.setColor(spectrum[k]);
                    if (k == currentPoint) {
                        if (WindowManager.getCurrentImage() == imp) {
                            g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                                    ic.screenY(p.y - 1) + dy,
                                    ic.screenX(p.x - 1) + dx,
                                    ic.screenY(p.y - 1) + dy);
                            g.drawLine(ic.screenX(p.x - 1) + dx,
                                    ic.screenY(p.y - 1) + dy,
                                    ic.screenX(p.x - 1) + dx,
                                    ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
                            g.drawLine(ic.screenX(p.x - 1) + dx,
                                    ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
                                    ic.screenX(p.x + 1) + dx,
                                    ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
                            g.drawLine(ic.screenX(p.x + 1) + dx,
                                    ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
                                    ic.screenX(p.x + 1) + dx,
                                    ic.screenY(p.y - 1) + dy);
                            g.drawLine(ic.screenX(p.x + 1) + dx,
                                    ic.screenY(p.y - 1) + dy,
                                    ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                                    ic.screenY(p.y - 1) + dy);
                            g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                                    ic.screenY(p.y - 1) + dy,
                                    ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                                    ic.screenY(p.y + 1) + dy);
                            g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
                                    ic.screenY(p.y + 1) + dy,
                                    ic.screenX(p.x + 1) + dx,
                                    ic.screenY(p.y + 1) + dy);
                            g.drawLine(ic.screenX(p.x + 1) + dx,
                                    ic.screenY(p.y + 1) + dy,
                                    ic.screenX(p.x + 1) + dx,
                                    ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
                            g.drawLine(ic.screenX(p.x + 1) + dx,
                                    ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
                                    ic.screenX(p.x - 1) + dx,
                                    ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
                            g.drawLine(ic.screenX(p.x - 1) + dx,
                                    ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
                                    ic.screenX(p.x - 1) + dx,
                                    ic.screenY(p.y + 1) + dy);
                            g.drawLine(ic.screenX(p.x - 1) + dx,
                                    ic.screenY(p.y + 1) + dy,
                                    ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                                    ic.screenY(p.y + 1) + dy);
                            g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                                    ic.screenY(p.y + 1) + dy,
                                    ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
                                    ic.screenY(p.y - 1) + dy);
                            if (1.0 < ic.getMagnification()) {
                                g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
                                        ic.screenY(p.y) + dy,
                                        ic.screenX(p.x + CROSS_HALFSIZE) + dx,
                                        ic.screenY(p.y) + dy);
                                g.drawLine(ic.screenX(p.x) + dx,
                                        ic.screenY(p.y - CROSS_HALFSIZE) + dy,
                                        ic.screenX(p.x) + dx,
                                        ic.screenY(p.y + CROSS_HALFSIZE) + dy);
                            }
                        } else {
                            g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
                                    ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
                                    ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
                                    ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
                            g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
                                    ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
                                    ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
                                    ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
                        }
                    } else {
                        g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
                                ic.screenY(p.y) + dy,
                                ic.screenX(p.x + CROSS_HALFSIZE) + dx,
                                ic.screenY(p.y) + dy);
                        g.drawLine(ic.screenX(p.x) + dx,
                                ic.screenY(p.y - CROSS_HALFSIZE) + dy,
                                ic.screenX(p.x) + dx,
                                ic.screenY(p.y + CROSS_HALFSIZE) + dy);
                    }
                }
            }
            if (updateFullWindow) {
                updateFullWindow = false;
                imp.draw();
            }
        }
    }

    /**
     * Set the current point as that which is closest to (x, y).
     *
     * @param x Horizontal coordinate in canvas units.
     * @param y Vertical coordinate in canvas units.
     **/
    public int findClosest(
            int x,
            int y
    ) {
        x = ic.offScreenX(x);
        y = ic.offScreenY(y);
        int closest = 0;
        Point p = point[closest];
        double distance = (double) (x - p.x) * (double) (x - p.x)
                + (double) (y - p.y) * (double) (y - p.y);
        double candidate;
        if (transformation == TurboRegTransformation.RigidBody) {
            for (int k = 1; (k < transformation.getNativeValue()); k++) {
                p = point[k];
                candidate = (double) (x - p.x) * (double) (x - p.x)
                        + (double) (y - p.y) * (double) (y - p.y);
                if (candidate < distance) {
                    distance = candidate;
                    closest = k;
                }
            }
        } else {
            for (int k = 1; (k < (transformation.getNativeValue() / 2)); k++) {
                p = point[k];
                candidate = (double) (x - p.x) * (double) (x - p.x)
                        + (double) (y - p.y) * (double) (y - p.y);
                if (candidate < distance) {
                    distance = candidate;
                    closest = k;
                }
            }
        }
        currentPoint = closest;
        return (currentPoint);
    }

    /**
     * Return the current point as a <code>Point</code> object.
     **/
    public Point getPoint(
    ) {
        return (point[currentPoint]);
    }

    /**
     * Return all landmarks as an array <code>double[transformation / 2][2]</code>,
     * except for a rigid-body transformation for which the array has size
     * <code>double[3][2]</code>.
     **/
    public double[][] getPoints(
    ) {
        if (interactive) {
            if (transformation == TurboRegTransformation.RigidBody) {
                double[][] points = new double[transformation.getNativeValue()][2];
                for (int k = 0; (k < transformation.getNativeValue()); k++) {
                    points[k][0] = (double) point[k].x;
                    points[k][1] = (double) point[k].y;
                }
                return (points);
            } else {
                double[][] points = new double[transformation.getNativeValue() / 2][2];
                for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                    points[k][0] = (double) point[k].x;
                    points[k][1] = (double) point[k].y;
                }
                return (points);
            }
        } else {
            return (precisionPoint);
        }
    }

    /**
     * Modify the location of the current point. Clip the admissible range
     * to the image size.
     *
     * @param x Desired new horizontal coordinate in canvas units.
     * @param y Desired new vertical coordinate in canvas units.
     **/
    public void movePoint(
            int x,
            int y
    ) {
        interactive = true;
        x = ic.offScreenX(x);
        y = ic.offScreenY(y);
        x = (x < 0) ? (0) : (x);
        x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
        y = (y < 0) ? (0) : (y);
        y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
        if ((transformation == TurboRegTransformation.RigidBody) && (currentPoint != 0)) {
            final Point p = new Point(x, y);
            final Point q = point[3 - currentPoint];
            final double radius = 0.5 * Math.sqrt(
                    (ic.screenX(p.x) - ic.screenX(q.x))
                            * (ic.screenX(p.x) - ic.screenX(q.x))
                            + (ic.screenY(p.y) - ic.screenY(q.y))
                            * (ic.screenY(p.y) - ic.screenY(q.y)));
            if ((double) CROSS_HALFSIZE < radius) {
                point[currentPoint].x = x;
                point[currentPoint].y = y;
            }
        } else {
            point[currentPoint].x = x;
            point[currentPoint].y = y;
        }
    }

    /**
     * Set a new current point.
     *
     * @param currentPoint New current point index.
     **/
    public void setCurrentPoint(
            final int currentPoint
    ) {
        this.currentPoint = currentPoint;
    }

    /**
     * Set new position for all landmarks, without clipping.
     *
     * @param precisionPoint New coordinates in canvas units.
     **/
    public void setPoints(
            final double[][] precisionPoint
    ) {
        interactive = false;
        if (transformation == TurboRegTransformation.RigidBody) {
            for (int k = 0; (k < transformation.getNativeValue()); k++) {
                point[k].x = (int) Math.round(precisionPoint[k][0]);
                point[k].y = (int) Math.round(precisionPoint[k][1]);
                this.precisionPoint[k][0] = precisionPoint[k][0];
                this.precisionPoint[k][1] = precisionPoint[k][1];
            }
        } else {
            for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                point[k].x = (int) Math.round(precisionPoint[k][0]);
                point[k].y = (int) Math.round(precisionPoint[k][1]);
                this.precisionPoint[k][0] = precisionPoint[k][0];
                this.precisionPoint[k][1] = precisionPoint[k][1];
            }
        }
    }

    /**
     * Reset the landmarks to their initial position for the given
     * transformation.
     *
     * @param transformation Transformation code.
     **/
    public void setTransformation(
            final TurboRegTransformation transformation
    ) {
        interactive = true;
        this.transformation = transformation;
        final int width = imp.getWidth();
        final int height = imp.getHeight();
        currentPoint = 0;
        switch (transformation) {
            case Translation: {
                point[0] = new Point(
                        Math.round((float) (Math.floor(0.5 * (double) width))),
                        Math.round((float) (Math.floor(0.5 * (double) height))));
                break;
            }
            case RigidBody: {
                point[0] = new Point(
                        Math.round((float) (Math.floor(0.5 * (double) width))),
                        Math.round((float) (Math.floor(0.5 * (double) height))));
                point[1] = new Point(
                        Math.round((float) (Math.floor(0.5 * (double) width))),
                        Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) height))));
                point[2] = new Point(
                        Math.round((float) (Math.floor(0.5 * (double) width))),
                        height - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) height))));
                break;
            }
            case ScaledRotation: {
                point[0] = new Point(
                        Math.round((float) (Math.floor(0.25 * GOLDEN_RATIO
                                * (double) width))),
                        Math.round((float) (Math.floor(0.5 * (double) height))));
                point[1] = new Point(
                        width - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) width))),
                        Math.round((float) (Math.floor(0.5 * (double) height))));
                break;
            }
            case Affine: {
                point[0] = new Point(
                        Math.round((float) (Math.floor(0.5 * (double) width))),
                        Math.round((float) (Math.floor(0.25 * GOLDEN_RATIO
                                * (double) height))));
                point[1] = new Point(
                        Math.round((float) (Math.floor(0.25 * GOLDEN_RATIO
                                * (double) width))),
                        height - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) height))));
                point[2] = new Point(
                        width - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) width))),
                        height - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) height))));
                break;
            }
            case Bilinear: {
                point[0] = new Point(
                        Math.round((float) (Math.floor(0.25 * GOLDEN_RATIO
                                * (double) width))),
                        Math.round((float) (Math.floor(0.25 * GOLDEN_RATIO
                                * (double) height))));
                point[1] = new Point(
                        Math.round((float) (Math.floor(0.25 * GOLDEN_RATIO
                                * (double) width))),
                        height - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) height))));
                point[2] = new Point(
                        width - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) width))),
                        Math.round((float) (Math.floor(0.25 * GOLDEN_RATIO
                                * (double) height))));
                point[3] = new Point(
                        width - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) width))),
                        height - Math.round((float) (Math.ceil(0.25 * GOLDEN_RATIO
                                * (double) height))));
                break;
            }
        }
        setSpectrum();
        imp.updateAndDraw();
    }

    /**
     * Keep a local copy of the points and of the transformation.
     **/
    public TurboRegPointHandler(
            final double[][] precisionPoint,
            final TurboRegTransformation transformation
    ) {
        super(0, 0, 0, 0, null);
        this.transformation = transformation;
        this.precisionPoint = precisionPoint;
        interactive = false;
    }

    /**
     * Keep a local copy of the <code>ImagePlus</code> object. Set the
     * landmarks to their initial position for the given transformation.
     *
     * @param imp            <code>ImagePlus</code> object.
     * @param transformation Transformation code.
     **/
    public TurboRegPointHandler(
            final ImagePlus imp,
            final TurboRegTransformation transformation
    ) {
        super(0, 0, imp.getWidth(), imp.getHeight(), imp);
        this.transformation = transformation;
        setTransformation(transformation);
        imp.setRoi(this);
        started = true;
    }

    private void drawArcs(
            final Graphics g
    ) {
        final double mag = ic.getMagnification();
        final int dx = (int) (mag / 2.0);
        final int dy = (int) (mag / 2.0);
        final Point p = point[1];
        final Point q = point[2];
        final double x0 = (double) (ic.screenX(p.x) + ic.screenX(q.x));
        final double y0 = (double) (ic.screenY(p.y) + ic.screenY(q.y));
        final double dx0 = (double) (ic.screenX(p.x) - ic.screenX(q.x));
        final double dy0 = (double) (ic.screenY(p.y) - ic.screenY(q.y));
        final double radius = 0.5 * Math.sqrt(dx0 * dx0 + dy0 * dy0);
        final double orientation = Math.atan2(dx0, dy0);
        final double spacerAngle = Math.asin((double) CROSS_HALFSIZE / radius);
        g.setColor(spectrum[1]);
        g.drawArc((int) Math.round(0.5 * x0 - radius) + dx,
                (int) Math.round(0.5 * y0 - radius) + dy,
                (int) Math.round(2.0 * radius), (int) Math.round(2.0 * radius),
                (int) Math.round((orientation + spacerAngle + Math.PI)
                        * 180.0 / Math.PI),
                (int) Math.round((Math.PI - 2.0 * spacerAngle) * 180.0 / Math.PI));
        g.setColor(spectrum[2]);
        g.drawArc((int) Math.round(0.5 * x0 - radius) + dx,
                (int) Math.round(0.5 * y0 - radius) + dy,
                (int) Math.round(2.0 * radius), (int) Math.round(2.0 * radius),
                (int) Math.round((orientation + spacerAngle) * 180.0 / Math.PI),
                (int) Math.round((Math.PI - 2.0 * spacerAngle) * 180.0 / Math.PI));
    }


    private void drawHorizon(
            final Graphics g
    ) {
        final double mag = ic.getMagnification();
        final int dx = (int) (mag / 2.0);
        final int dy = (int) (mag / 2.0);
        final Point p = point[1];
        final Point q = point[2];
        final double x0 = (double) (ic.screenX(p.x) + ic.screenX(q.x));
        final double y0 = (double) (ic.screenY(p.y) + ic.screenY(q.y));
        final double dx0 = (double) (ic.screenX(p.x) - ic.screenX(q.x));
        final double dy0 = (double) (ic.screenY(p.y) - ic.screenY(q.y));
        final double radius = 0.5 * Math.sqrt(dx0 * dx0 + dy0 * dy0);
        final double spacerAngle = Math.asin((double) CROSS_HALFSIZE / radius);
        final double s0 = Math.sin(spacerAngle);
        final double s = 0.5 * dx0 / radius;
        final double c = 0.5 * dy0 / radius;
        double u;
        double v;
        g.setColor(spectrum[1]);
        u = 0.5 * (x0 + s0 * dx0);
        v = 0.5 * (y0 + s0 * dy0);
        if (Math.abs(s) < Math.abs(c)) {
            g.drawLine(-dx, (int) Math.round(
                            v + (u + 2.0 * (double) dx) * s / c) + dy,
                    (int) Math.round(mag * (double) ic.getSrcRect().width - 1.0) + dx,
                    (int) Math.round(v - (mag * (double) ic.getSrcRect().width - 1.0 - u)
                            * s / c) + dy);
        } else {
            g.drawLine((int) Math.round(
                            u + (v + 2.0 * (double) dy) * c / s) + dx, -dy,
                    (int) Math.round(u - (mag * (double) ic.getSrcRect().height - 1.0 - v)
                            * c / s) + dx,
                    (int) Math.round(mag * (double) ic.getSrcRect().height - 1.0) + dy);
        }
        g.setColor(spectrum[2]);
        u = 0.5 * (x0 - s0 * dx0);
        v = 0.5 * (y0 - s0 * dy0);
        if (Math.abs(s) < Math.abs(c)) {
            g.drawLine(-dx, (int) Math.round(
                            v + (u + 2.0 * (double) dx) * s / c) + dy,
                    (int) Math.round(mag * (double) ic.getSrcRect().width - 1.0) + dx,
                    (int) Math.round(v - (mag * (double) ic.getSrcRect().width - 1.0 - u)
                            * s / c) + dy);
        } else {
            g.drawLine((int) Math.round(
                            u + (v + 2.0 * (double) dy) * c / s) + dx, -dy,
                    (int) Math.round(u - (mag * (double) ic.getSrcRect().height - 1.0 - v)
                            * c / s) + dx, (int) Math.round(
                            mag * (double) ic.getSrcRect().height - 1.0) + dy);
        }
    }

    private void setSpectrum(
    ) {
        if (transformation == TurboRegTransformation.RigidBody) {
            spectrum[0] = Color.green;
            spectrum[1] = new Color(16, 119, 169);
            spectrum[2] = new Color(119, 85, 51);
        } else {
            spectrum[0] = Color.green;
            spectrum[1] = Color.yellow;
            spectrum[2] = Color.magenta;
            spectrum[3] = Color.cyan;
        }
    }
}
