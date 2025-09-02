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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.utils.PointRange;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Set;

public class JIPipeDesktopGraphEdgeUI implements JIPipeDesktopGraphInteractiveObjectUI, Comparable<JIPipeDesktopGraphEdgeUI> {
    private final JIPipeDataSlot source;
    private final JIPipeDataSlot target;
    private final JIPipeGraphEdge edge;

    private int multiColorIndex;

    private int multiColorMax;

    private JIPipeDesktopGraphNodeUI sourceUI;

    private JIPipeDesktopGraphNodeUI targetUI;

    private Point sourceCenter;

    private Point targetCenter;

    private PointRange sourcePoint;

    private PointRange targetPoint;

    private boolean hidden;

    public JIPipeDesktopGraphEdgeUI(JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge) {
        this.source = source;
        this.target = target;
        this.edge = edge;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public int getMultiColorMax() {
        return multiColorMax;
    }

    public void setMultiColorMax(int multiColorMax) {
        this.multiColorMax = multiColorMax;
    }

    public Point getSourceCenter() {
        return sourceCenter;
    }

    public void setSourceCenter(Point sourceCenter) {
        this.sourceCenter = sourceCenter;
    }

    public Point getTargetCenter() {
        return targetCenter;
    }

    public void setTargetCenter(Point targetCenter) {
        this.targetCenter = targetCenter;
    }

    public JIPipeGraphEdge getEdge() {
        return edge;
    }

    public JIPipeDataSlot getSource() {
        return source;
    }

    public JIPipeDataSlot getTarget() {
        return target;
    }

    public int getMultiColorIndex() {
        return multiColorIndex;
    }

    public void setMultiColorIndex(int multiColorIndex) {
        this.multiColorIndex = multiColorIndex;
    }

    public PointRange getSourcePoint() {
        return sourcePoint;
    }

    public void setSourcePoint(PointRange sourcePoint) {
        this.sourcePoint = sourcePoint;
    }

    public PointRange getTargetPoint() {
        return targetPoint;
    }

    public void setTargetPoint(PointRange targetPoint) {
        this.targetPoint = targetPoint;
    }

    public boolean isCommentEdge() {
        return source.getNode() instanceof JIPipeCommentNode || target.getNode() instanceof JIPipeCommentNode;
    }

    public JIPipeDesktopGraphNodeUI getSourceUI() {
        return sourceUI;
    }

    public void setSourceUI(JIPipeDesktopGraphNodeUI sourceUI) {
        this.sourceUI = sourceUI;
    }

    public JIPipeDesktopGraphNodeUI getTargetUI() {
        return targetUI;
    }

    public void setTargetUI(JIPipeDesktopGraphNodeUI targetUI) {
        this.targetUI = targetUI;
    }

    public int getUIManhattanDistance() {
        if (sourcePoint != null && targetPoint != null) {
            return Math.abs(sourcePoint.center.x - targetPoint.center.x) + Math.abs(sourcePoint.center.y - targetPoint.center.y);
        } else {
            return -1;
        }
    }

    @Override
    public int compareTo(@NotNull JIPipeDesktopGraphEdgeUI o) {
        return Integer.compare(getUIManhattanDistance(), o.getUIManhattanDistance());
    }

    @Override
    public Set<JIPipeGraphNode> getNodes() {
        return Set.of(sourceUI.getNode(), targetUI.getNode());
    }

    @Override
    public void updateView(JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand command) {

    }
}
