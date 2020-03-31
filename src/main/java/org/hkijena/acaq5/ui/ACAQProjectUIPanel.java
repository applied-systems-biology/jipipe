package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQProject;

import javax.swing.*;

/**
 * Panel that holds a reference to {@link ACAQProjectUI}
 */
public class ACAQProjectUIPanel extends JPanel {

    private final ACAQProjectUI workbenchUI;

    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQProjectUIPanel(ACAQProjectUI workbenchUI) {
        this.workbenchUI = workbenchUI;
    }

    /**
     * @return The workbench UI
     */
    public ACAQProjectUI getWorkbenchUI() {
        return workbenchUI;
    }

    /**
     * @return The project
     */
    public ACAQProject getProject() {
        return workbenchUI.getProject();
    }
}
