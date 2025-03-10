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

package org.hkijena.jipipe.api.nodes.annotation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.awt.*;
import java.util.Set;

/**
 * A node that is an annotation
 * Nodes of this type are rendered by {@link JIPipeDesktopAnnotationGraphNodeUI} instead of {@link JIPipeDesktopGraphNodeUI} and have a custom size information
 */
public abstract class JIPipeAnnotationGraphNode extends JIPipeGraphNode {

    private int gridWidth = 4;
    private int gridHeight = 3;
    private int zOrder = 1; // default value pushes the node in front of all others

    public JIPipeAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().seal().build());
    }

    public JIPipeAnnotationGraphNode(JIPipeAnnotationGraphNode other) {
        super(other);
        this.gridWidth = other.gridWidth;
        this.gridHeight = other.gridHeight;
    }

    @SetJIPipeDocumentation(name = "Width", description = "Width of this node in grid coordinates")
    @JIPipeParameter(value = "grid-width", uiOrder = 1000)
    public int getGridWidth() {
        return gridWidth;
    }

    @JIPipeParameter("grid-width")
    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
    }

    @SetJIPipeDocumentation(name = "Height", description = "Width of this node in grid coordinates")
    @JIPipeParameter(value = "grid-height", uiOrder = 1010)
    public int getGridHeight() {
        return gridHeight;
    }

    @JIPipeParameter("grid-height")
    public void setGridHeight(int gridHeight) {
        this.gridHeight = gridHeight;
    }

    @SetJIPipeDocumentation(name = "Z-Order", description = "Determines the Z order of this annotation. This is an internal value and will be updated automatically.")
    @JIPipeParameter(value = "z-order", hidden = true)
    public int getzOrder() {
        return zOrder;
    }

    @JIPipeParameter("z-order")
    public void setzOrder(int zOrder) {
        this.zOrder = zOrder;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {

    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

    }

    public boolean isDrawWithAntialiasing() {
        return false;
    }

    public boolean isPaintNodeShadow() {
        return true;
    }

    public abstract void paintNode(Graphics2D g2, JIPipeDesktopAnnotationGraphNodeUI nodeUI, double zoom);

    public void paintMinimap(Graphics2D graphics2D, int x, int y, int width, int height, BasicStroke defaultStroke, BasicStroke selectedStroke, Set<JIPipeDesktopGraphNodeUI> selection) {
        graphics2D.setColor(Color.GRAY);
        graphics2D.setStroke(JIPipeDesktopGraphCanvasUI.STROKE_MARQUEE);
        graphics2D.drawRect(x, y, width, height);
    }
}
