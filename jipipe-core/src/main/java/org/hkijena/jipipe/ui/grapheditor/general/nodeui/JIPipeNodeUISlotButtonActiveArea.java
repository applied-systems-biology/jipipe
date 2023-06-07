package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

public class JIPipeNodeUISlotButtonActiveArea extends JIPipeNodeUIActiveArea {
    private final JIPipeNodeUISlotActiveArea uiSlot;

    public JIPipeNodeUISlotButtonActiveArea(JIPipeGraphNodeUI nodeUI, JIPipeNodeUISlotActiveArea uiSlot) {
        super(nodeUI, 50);
        this.uiSlot = uiSlot;
    }

    public JIPipeNodeUISlotActiveArea getUISlot() {
        return uiSlot;
    }
}
