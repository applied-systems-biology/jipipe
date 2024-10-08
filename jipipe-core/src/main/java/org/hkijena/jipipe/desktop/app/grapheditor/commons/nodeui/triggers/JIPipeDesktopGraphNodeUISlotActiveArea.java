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

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUIActiveArea;

import java.awt.*;

/**
 * Active area that stores the state of a slot
 */
public class JIPipeDesktopGraphNodeUISlotActiveArea extends JIPipeDesktopGraphNodeUIActiveArea {

    private final JIPipeSlotType slotType;
    private final String slotName;

    private final JIPipeDataSlot slot;

    private String slotLabel;

    private boolean slotLabelIsCustom;
    private JIPipeDesktopGraphNodeUI.SlotStatus slotStatus = JIPipeDesktopGraphNodeUI.SlotStatus.Default;

    private Point nativeLocation;

    private double nativeWidth;

    private Image icon;
    private Rectangle lastFillRect;

    public JIPipeDesktopGraphNodeUISlotActiveArea(JIPipeDesktopGraphNodeUI nodeUI, JIPipeSlotType slotType, String slotName, JIPipeDataSlot slot) {
        super(nodeUI, 100);
        this.slotType = slotType;
        this.slotName = slotName;
        this.slot = slot;
    }

    public JIPipeSlotType getSlotType() {
        return slotType;
    }

    public String getSlotLabel() {
        return slotLabel;
    }

    public void setSlotLabel(String slotLabel) {
        this.slotLabel = slotLabel;
    }

    public String getSlotName() {
        return slotName;
    }

    public JIPipeDesktopGraphNodeUI.SlotStatus getSlotStatus() {
        return slotStatus;
    }

    public void setSlotStatus(JIPipeDesktopGraphNodeUI.SlotStatus slotStatus) {
        this.slotStatus = slotStatus;
    }

    public boolean isSlotLabelIsCustom() {
        return slotLabelIsCustom;
    }

    public void setSlotLabelIsCustom(boolean slotLabelIsCustom) {
        this.slotLabelIsCustom = slotLabelIsCustom;
    }

    public double getNativeWidth() {
        return nativeWidth;
    }

    public void setNativeWidth(double nativeWidth) {
        this.nativeWidth = nativeWidth;
    }

    public Image getIcon() {
        return icon;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
    }

    public Point getNativeLocation() {
        return nativeLocation;
    }

    public void setNativeLocation(Point nativeLocation) {
        this.nativeLocation = nativeLocation;
    }

    @Override
    public String toString() {
        return "Slot State: " + slotName;
    }

    public JIPipeDataSlot getSlot() {
        return slot;
    }

    public boolean isInput() {
        return slotType == JIPipeSlotType.Input;
    }

    public boolean isOutput() {
        return slotType == JIPipeSlotType.Output;
    }

    public Rectangle getLastFillRect() {
        return lastFillRect;
    }

    public void setLastFillRect(Rectangle lastFillRect) {
        this.lastFillRect = lastFillRect;
    }
}
