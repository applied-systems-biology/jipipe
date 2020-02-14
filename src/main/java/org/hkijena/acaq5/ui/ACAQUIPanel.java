package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQProject;

import javax.swing.*;

public class ACAQUIPanel extends JPanel {

    private final ACAQWorkbenchUI workbenchUI;

    public ACAQUIPanel(ACAQWorkbenchUI workbenchUI) {
        this.workbenchUI = workbenchUI;
    }

    public ACAQWorkbenchUI getWorkbenchUI() {
        return workbenchUI;
    }

    public ACAQProject getProject() {
        return workbenchUI.getProject();
    }
}
