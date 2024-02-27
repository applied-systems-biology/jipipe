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

import org.hkijena.jipipe.api.data.JIPipeSlotType;

import java.awt.*;

public class JIPipeNodeUIAddSlotButtonActiveArea extends JIPipeNodeUIActiveArea {
    private final JIPipeSlotType slotType;

    private Point nativeLocation;

    private double nativeWidth;

    public JIPipeNodeUIAddSlotButtonActiveArea(JIPipeGraphNodeUI nodeUI, JIPipeSlotType slotType) {
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
