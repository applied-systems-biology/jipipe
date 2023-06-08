package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeAnnotationGraphNodeUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;

import java.awt.*;
import java.util.Set;

/**
 * A node that is an annotation
 * Nodes of this type are rendered by {@link org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeAnnotationGraphNodeUI} instead of {@link org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI} and have a custom size information
 */
public abstract class JIPipeAnnotationGraphNode extends JIPipeGraphNode {

    private int gridWidth = 4;
    private int gridHeight = 3;

    public JIPipeAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().seal().build());
    }

    public JIPipeAnnotationGraphNode(JIPipeAnnotationGraphNode other) {
        super(other);
        this.gridWidth = other.gridWidth;
        this.gridHeight = other.gridHeight;
    }

    @JIPipeDocumentation(name = "Width", description = "Width of this node in grid coordinates")
    @JIPipeParameter("grid-width")
    public int getGridWidth() {
        return gridWidth;
    }

    @JIPipeParameter("grid-width")
    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
    }

    @JIPipeDocumentation(name = "Height", description = "Width of this node in grid coordinates")
    @JIPipeParameter("grid-height")
    public int getGridHeight() {
        return gridHeight;
    }

    @JIPipeParameter("grid-height")
    public void setGridHeight(int gridHeight) {
        this.gridHeight = gridHeight;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {

    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {

    }

    public boolean isPaintNodeShadow() {
        return true;
    }

    public abstract void paintNode(Graphics2D g2, JIPipeAnnotationGraphNodeUI nodeUI);

    public void paintMinimap(Graphics2D graphics2D, int x, int y, int width, int height, BasicStroke defaultStroke, BasicStroke selectedStroke, Set<JIPipeGraphNodeUI> selection) {
        graphics2D.setColor(Color.GRAY);
        graphics2D.setStroke(JIPipeGraphCanvasUI.STROKE_MARQUEE);
        graphics2D.drawRect(x,y,width, height);
    }
}
