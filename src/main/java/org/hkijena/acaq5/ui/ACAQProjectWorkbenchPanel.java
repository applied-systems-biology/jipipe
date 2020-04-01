package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQProject;

import javax.swing.*;

/**
 * Panel that holds a reference to {@link ACAQProjectWorkbench}
 */
public class ACAQProjectWorkbenchPanel extends ACAQWorkbenchPanel {

    private final ACAQProjectWorkbench workbenchUI;

    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQProjectWorkbenchPanel(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        this.workbenchUI = workbenchUI;
    }

    /**
     * @return The workbench UI
     */
    public ACAQProjectWorkbench getProjectWorkbench() {
        return workbenchUI;
    }

    /**
     * @return The project
     */
    public ACAQProject getProject() {
        return workbenchUI.getProject();
    }
}
