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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grapheditortool.JIPipeToggleableGraphEditorTool;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class JIPipeAnnotationGraphNodeTool<T extends JIPipeAnnotationGraphNode> implements JIPipeToggleableGraphEditorTool, JIPipeWorkbenchAccess {
    private final Class<T> nodeClass;
    private final JIPipeAnnotationGraphNode nodeInstance;
    private JIPipeGraphEditorUI graphEditorUI;

    private Point firstPoint;

    private Point secondPoint;

    public JIPipeAnnotationGraphNodeTool(Class<T> nodeClass) {
        this.nodeClass = nodeClass;
        this.nodeInstance = JIPipe.createNode(nodeClass);
    }

    @Override
    public String getName() {
        return nodeInstance.getName();
    }

    @Override
    public String getTooltip() {
        return nodeInstance.getInfo().getDescription().getBody();
    }

    @Override
    public Icon getIcon() {
        return nodeInstance.getInfo().getIcon();
    }

    @Override
    public JIPipeGraphEditorUI getGraphEditor() {
        return graphEditorUI;
    }

    @Override
    public void setGraphEditor(JIPipeGraphEditorUI graphEditorUI) {
        this.graphEditorUI = graphEditorUI;
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void activate() {

    }

    @Override
    public int getCategory() {
        return 64;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            firstPoint = getGraphCanvas().getViewMode().realLocationToGrid(e.getPoint(), getGraphCanvas().getZoom());
            e.consume();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (firstPoint != null && secondPoint != null && !Objects.equals(firstPoint, secondPoint)) {
                createNodeAtPoint();
            }
            deactivate();
            getGraphCanvas().repaint(50);
            e.consume();
        }
    }

    private void createNodeAtPoint() {

        T newNode = createAndConfigureNode(firstPoint, secondPoint);
        getGraphCanvas().getGraph().insertNode(newNode, getGraphEditor().getCompartment());
    }

    protected T createAndConfigureNode(Point firstPoint, Point secondPoint) {
        int x = Math.min(firstPoint.x, secondPoint.x);
        int y = Math.min(firstPoint.y, secondPoint.y);
        int w = Math.abs(firstPoint.x - secondPoint.x);
        int h = Math.abs(firstPoint.y - secondPoint.y);
        T newNode = JIPipe.createNode(nodeClass);
        newNode.setGridWidth(w);
        newNode.setGridHeight(h);
        newNode.setLocationWithin(getGraphEditor().getCompartment(), new Point(x, y), getGraphEditor().getCanvasUI().getViewMode().name());
        return newNode;
    }

    public Class<T> getNodeClass() {
        return nodeClass;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        deactivate();
        getGraphCanvas().repaint(50);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        deactivate();
        getGraphCanvas().repaint(50);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (firstPoint != null) {
            secondPoint = getGraphCanvas().getViewMode().realLocationToGrid(e.getPoint(), getGraphCanvas().getZoom());
            getGraphCanvas().repaintLowLag();
            e.consume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void deactivate() {
        firstPoint = null;
        secondPoint = null;
    }

    @Override
    public boolean allowsDragNodes() {
        return false;
    }

    @Override
    public boolean allowsDragConnections() {
        return false;
    }

    @Override
    public void paintAfterNodesAndEdges(Graphics2D graphics2D) {
        if (firstPoint != null && secondPoint != null && !Objects.equals(firstPoint, secondPoint)) {
            int gridWidth = getGraphCanvas().getViewMode().getGridWidth();
            int gridHeight = getGraphCanvas().getViewMode().getGridHeight();
            double zoom = getGraphCanvas().getZoom();
            graphics2D.setStroke(JIPipeGraphCanvasUI.STROKE_COMMENT);
            graphics2D.setColor(JIPipeGraphCanvasUI.COLOR_HIGHLIGHT_GREEN);
            int x0 = (int) ((firstPoint.x * gridWidth) * zoom);
            int y0 = (int) ((firstPoint.y * gridHeight) * zoom);
            int x1 = (int) ((secondPoint.x * gridWidth) * zoom);
            int y1 = (int) ((secondPoint.y * gridHeight) * zoom);
            paintDragOverlay(graphics2D, x0, y0, x1, y1);
        }
    }

    protected void paintDragOverlay(Graphics2D graphics2D, int x0, int y0, int x1, int y1) {
        int x = Math.min(x0, x1);
        int y = Math.min(y0, y1);
        int w = Math.abs(x0 - x1);
        int h = Math.abs(y0 - y1);
        graphics2D.drawRect(x, y, w, h);
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return graphEditorUI.getWorkbench();
    }
}
