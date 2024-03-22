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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers;

import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUIActiveArea;

import java.awt.*;

public class JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea extends JIPipeDesktopGraphNodeUIActiveArea {
    private final JIPipeSlotType slotType;

    private Point nativeLocation;

    private double nativeWidth;

    public JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea(JIPipeDesktopGraphNodeUI nodeUI, JIPipeSlotType slotType) {
        super(nodeUI, 50);
        this.slotType = slotType;
    }

    public JIPipeSlotType getSlotType() {
        return slotType;
    }

    public Point getNativeLocation() {
        return nativeLocation;
    }

    public void setNativeLocation(Point nativeLocation) {
        this.nativeLocation = nativeLocation;
    }

    public double getNativeWidth() {
        return nativeWidth;
    }

    public void setNativeWidth(double nativeWidth) {
        this.nativeWidth = nativeWidth;
    }
}
