package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.utils.PointRange;

import java.awt.*;

public class JIPipeNodeUIActiveArea implements Comparable<JIPipeNodeUIActiveArea> {

    private final JIPipeNodeUI nodeUI;
    private final int priority;

    private Rectangle zoomedHitArea;

    public JIPipeNodeUIActiveArea(JIPipeNodeUI nodeUI, int priority) {
        this.nodeUI = nodeUI;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(JIPipeNodeUIActiveArea o) {
        return Integer.compare(priority, o.priority);
    }

    public Rectangle getZoomedHitArea() {
        return zoomedHitArea;
    }

    public void setZoomedHitArea(Rectangle zoomedHitArea) {
        this.zoomedHitArea = zoomedHitArea;
    }

    public JIPipeNodeUI getNodeUI() {
        return nodeUI;
    }

    public PointRange getZoomedHitAreaCenter() {
        return new PointRange((int) (zoomedHitArea.getX() + zoomedHitArea.getWidth() / 2),
                (int) (zoomedHitArea.getY() + zoomedHitArea.getHeight() / 2));
    }
}
