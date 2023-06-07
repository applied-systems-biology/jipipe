package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;

import java.awt.*;

public class JIPipeAnnotationGraphNodeUI extends JIPipeGraphNodeUI {

    /**
     * Creates a new UI
     *
     * @param workbench     thr workbench
     * @param graphCanvasUI The graph UI that contains this UI
     * @param node          The algorithm
     */
    public JIPipeAnnotationGraphNodeUI(JIPipeWorkbench workbench, JIPipeGraphCanvasUI graphCanvasUI, JIPipeAnnotationGraphNode node) {
        super(workbench, graphCanvasUI, node);
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        updateView(false, false, true);
    }

    @Override
    protected void updateSize() {

        JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) getNode();

        // Update the real size of the control
        Dimension gridSize = new Dimension(Math.max(1, annotationGraphNode.getGridWidth()), Math.max(1, annotationGraphNode.getGridHeight()));
        Dimension realSize =  getViewMode().gridToRealSize(gridSize, getGraphCanvasUI().getZoom());
        setSize(realSize);
        revalidate();

        // Update the active areas
        updateActiveAreas();
    }

    @Override
    protected void updateActiveAreas() {
        activeAreas.clear();

        // Add whole node
        updateWholeNodeActiveAreas();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public boolean isDrawShadow() {
        JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) getNode();
        return annotationGraphNode.isPaintNodeShadow();
    }

    @Override
    protected void paintNode(Graphics2D g2) {
        JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) getNode();
        annotationGraphNode.paintNode(g2, this);
    }
}
