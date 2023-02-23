package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import java.awt.*;

public class JIPipeNodeUIActiveArea implements Comparable<JIPipeNodeUIActiveArea> {

    private final int priority;

    private Rectangle zoomedHitArea;

    public JIPipeNodeUIActiveArea(int priority) {
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
}
