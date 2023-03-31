package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;

import java.awt.*;

/**
 * Active area that stores the state of a slot
 */
public class JIPipeNodeUISlotActiveArea extends JIPipeNodeUIActiveArea {

    private final JIPipeSlotType slotType;
    private final String slotName;

    private final JIPipeDataSlot slot;

    private String slotLabel;

    private boolean slotLabelIsCustom;
    private JIPipeNodeUI.SlotStatus slotStatus = JIPipeNodeUI.SlotStatus.Default;

    private Point nativeLocation;

    private double nativeWidth;

    private Image icon;

    public JIPipeNodeUISlotActiveArea(JIPipeNodeUI nodeUI, JIPipeSlotType slotType, String slotName, JIPipeDataSlot slot) {
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

    public JIPipeNodeUI.SlotStatus getSlotStatus() {
        return slotStatus;
    }

    public void setSlotStatus(JIPipeNodeUI.SlotStatus slotStatus) {
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
}
