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

package org.hkijena.jipipe.plugins.graphannotation.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.awt.*;
import java.util.Set;

@SetJIPipeDocumentation(name = "Arrow", description = "An arrow")
@ConfigureJIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class ArrowAnnotationGraphNode extends JIPipeAnnotationGraphNode {

    private int angle = 0;
    private int arrowSize = 10;
    private Color color = new Color(0xD99323);
    private int lineThickness = 4;

    public ArrowAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);
    }

    public ArrowAnnotationGraphNode(ArrowAnnotationGraphNode other) {
        super(other);
        this.angle = other.angle;
        this.arrowSize = other.arrowSize;
        this.color = other.color;
        this.lineThickness = other.lineThickness;
    }

    @SetJIPipeDocumentation(name = "Angle", description = "The angle of the arrow in degrees")
    @JIPipeParameter("angle")
    public int getAngle() {
        return angle;
    }

    @JIPipeParameter("angle")
    public void setAngle(int angle) {
        this.angle = angle;
    }

    @SetJIPipeDocumentation(name = "Arrow head size", description = "The size of the arrow head")
    @JIPipeParameter("arrow-size")
    public int getArrowSize() {
        return arrowSize;
    }

    @JIPipeParameter("arrow-size")
    public void setArrowSize(int arrowSize) {
        this.arrowSize = arrowSize;
    }

    @SetJIPipeDocumentation(name = "Color", description = "Color of the arrow")
    @JIPipeParameter("color")
    public Color getColor() {
        return color;
    }

    @JIPipeParameter("color")
    public void setColor(Color color) {
        this.color = color;
    }

    @SetJIPipeDocumentation(name = "Thickness", description = "The thickness of the arrow")
    @JIPipeParameter("thickness")
    public int getLineThickness() {
        return lineThickness;
    }

    @JIPipeParameter("thickness")
    public void setLineThickness(int lineThickness) {
        this.lineThickness = lineThickness;
    }

    @Override
    public boolean isPaintNodeShadow() {
        return false;
    }

    private double[] calculateLineCoordinates(double w, double h) {
        double x0, y0, x1, y1;

        // Fix the angle and shift
        int finalAngle = angle;
        while (finalAngle < 0) {
            finalAngle += 360;
        }
        finalAngle %= 360;

        boolean invert = finalAngle > 90 && finalAngle <= 270;

        if (finalAngle == 0) {
            x0 = 0;
            y0 = h / 2;
            x1 = w;
            y1 = h / 2;
        } else if (Math.abs(finalAngle) % 180 == 0) {
            x0 = 0;
            y0 = h / 2;
            x1 = w;
            y1 = h / 2;
        } else if (Math.abs(finalAngle) % 90 == 0) {
            x0 = w / 2;
            y0 = 0;
            x1 = w / 2;
            y1 = h;
        } else {

            double m = Math.tan(Math.toRadians(finalAngle));
            double n = (h / 2) - m * (w / 2);

            double xz = -n / m;
            double xh = (h - n) / m;

            if (n < h && n >= 0) {
                x0 = 0;
                y0 = n;
                x1 = w;
                y1 = m * x1 + n;
            } else if (n < h) {
                x0 = xz;
                y0 = xz * m + n;
                x1 = xh;
                y1 = xh * m + n;
            } else {
                x0 = xh;
                y0 = xh * m + n;
                x1 = xz;
                y1 = xz * m + n;
            }
        }

        if (invert) {
            double sx = x0;
            double sy = y0;
            x0 = x1;
            y0 = y1;
            x1 = sx;
            y1 = sy;
        }

        return new double[]{x0, y0, x1, y1};
    }

    @Override
    public void paintNode(Graphics2D g2, JIPipeDesktopAnnotationGraphNodeUI nodeUI, double zoom) {
        int finalThickness = (int) Math.max(1, zoom * lineThickness);
        int finalArrowSize = (int) (zoom * arrowSize);
        double w = nodeUI.getWidth() - finalThickness * 2;
        double h = nodeUI.getHeight() - finalThickness * 2;

        double[] lineCoordinates = calculateLineCoordinates(w, h);
        int x0 = (int) lineCoordinates[0] + finalThickness;
        int y0 = (int) lineCoordinates[1] + finalThickness;
        int x1 = (int) lineCoordinates[2] + finalThickness;
        int y1 = (int) lineCoordinates[3] + finalThickness;

        g2.setStroke(new BasicStroke(finalThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color);
        g2.drawLine(x0, y0, x1, y1);

        if (finalArrowSize > 0) {
            // Draw arrowhead
            double angle = Math.atan2(y1 - y0, x1 - x0);
            int ax1 = (int) (x1 - arrowSize * Math.cos(angle - Math.PI / 6));
            int ay1 = (int) (y1 - arrowSize * Math.sin(angle - Math.PI / 6));
            int ax2 = (int) (x1 - arrowSize * Math.cos(angle + Math.PI / 6));
            int ay2 = (int) (y1 - arrowSize * Math.sin(angle + Math.PI / 6));
            g2.drawLine(x1, y1, ax1, ay1);
            g2.drawLine(x1, y1, ax2, ay2);
        }
    }

    @Override
    public void paintMinimap(Graphics2D graphics2D, int x, int y, int width, int height, BasicStroke defaultStroke, BasicStroke selectedStroke, Set<JIPipeDesktopGraphNodeUI> selection) {
        double[] lineCoordinates = calculateLineCoordinates(width, height);
        int x0 = (int) lineCoordinates[0];
        int y0 = (int) lineCoordinates[1];
        int x1 = (int) lineCoordinates[2];
        int y1 = (int) lineCoordinates[3];
        graphics2D.drawLine(x + x0, y + y0, x + x1, y + y1);
        if (arrowSize > 0) {
            int finalArrowSize = 5;
            double angle = Math.atan2(y1 - y0, x1 - x0);
            int ax1 = (int) (x1 - finalArrowSize * Math.cos(angle - Math.PI / 6));
            int ay1 = (int) (y1 - finalArrowSize * Math.sin(angle - Math.PI / 6));
            int ax2 = (int) (x1 - finalArrowSize * Math.cos(angle + Math.PI / 6));
            int ay2 = (int) (y1 - finalArrowSize * Math.sin(angle + Math.PI / 6));
            graphics2D.drawLine(x + x1, y + y1, x + ax1, y + ay1);
            graphics2D.drawLine(x + x1, y + y1, x + ax2, y + ay2);
        }
    }
}
