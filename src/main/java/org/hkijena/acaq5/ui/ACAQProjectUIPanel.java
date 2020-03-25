package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQProject;

import javax.swing.*;

public class ACAQProjectUIPanel extends JPanel {

    private final ACAQProjectUI workbenchUI;

    public ACAQProjectUIPanel(ACAQProjectUI workbenchUI) {
        this.workbenchUI = workbenchUI;
    }

    public ACAQProjectUI getWorkbenchUI() {
        return workbenchUI;
    }

    public ACAQProject getProject() {
        return workbenchUI.getProject();
    }
}
