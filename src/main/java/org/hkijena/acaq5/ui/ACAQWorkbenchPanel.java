package org.hkijena.acaq5.ui;

import javax.swing.*;

/**
 * Panel that contains a reference to its parent workbench
 */
public class ACAQWorkbenchPanel extends JPanel {
    private ACAQWorkbench workbench;

    /**
     * @param workbench the workbench
     */
    public ACAQWorkbenchPanel(ACAQWorkbench workbench) {
        this.workbench = workbench;
    }

    public ACAQWorkbench getWorkbench() {
        return workbench;
    }
}
