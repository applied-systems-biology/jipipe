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

package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.utils.PointRange;

import java.awt.*;

public class JIPipeNodeUIActiveArea implements Comparable<JIPipeNodeUIActiveArea> {

    private final JIPipeGraphNodeUI nodeUI;
    private final int priority;

    private Rectangle zoomedHitArea;

    public JIPipeNodeUIActiveArea(JIPipeGraphNodeUI nodeUI, int priority) {
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

    public JIPipeGraphNodeUI getNodeUI() {
        return nodeUI;
    }

    public PointRange getZoomedHitAreaCenter() {
        return new PointRange((int) (zoomedHitArea.getX() + zoomedHitArea.getWidth() / 2),
                (int) (zoomedHitArea.getY() + zoomedHitArea.getHeight() / 2));
    }
}
