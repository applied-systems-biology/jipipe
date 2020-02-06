package org.hkijena.acaq5.ui;

import javax.swing.*;

public class ACAQUIPanel extends JPanel {

    private final ACAQWorkbenchUI workbenchUI;

    public ACAQUIPanel(ACAQWorkbenchUI workbenchUI) {
        this.workbenchUI = workbenchUI;
    }

    public ACAQWorkbenchUI getWorkbenchUI() {
        return workbenchUI;
    }
}
