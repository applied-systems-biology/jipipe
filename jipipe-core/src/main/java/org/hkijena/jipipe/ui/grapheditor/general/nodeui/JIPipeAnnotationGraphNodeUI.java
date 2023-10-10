package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;

import java.awt.*;
import java.util.Set;

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
        setBuffered(false);
        setOpaque(false);
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
        Dimension realSize = getViewMode().gridToRealSize(gridSize, getGraphCanvasUI().getZoom());
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
    public boolean isDrawShadow() {
        JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) getNode();
        return annotationGraphNode.isPaintNodeShadow();
    }

    @Override
    protected void paintNode(Graphics2D g2) {
        // The graph canvas takes over drawing due to issues with edges & shadows
//        JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) getNode();
//        annotationGraphNode.paintNode(g2, this, getZoom());
    }

    public void setNodeGridSize(int width, int height) {
        width = Math.max(1, width);
        height = Math.max(1, height);
        JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) getNode();
        annotationGraphNode.setGridWidth(width);
        annotationGraphNode.setGridHeight(height);
        updateView(true, true, true);
    }

    @Override
    public void paintMinimap(Graphics2D graphics2D, int x, int y, int width, int height, BasicStroke defaultStroke, BasicStroke selectedStroke, Set<JIPipeGraphNodeUI> selection) {
        JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) getNode();
        annotationGraphNode.paintMinimap(graphics2D, x, y, width, height, defaultStroke, selectedStroke, selection);
    }
}
