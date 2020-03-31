package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQJsonExtension;

import javax.swing.*;

/**
 * UI panel that contains references to an {@link ACAQJsonExtension} UI
 */
public class ACAQJsonExtensionUIPanel extends JPanel {

    private final ACAQJsonExtensionUI workbenchUI;

    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQJsonExtensionUIPanel(ACAQJsonExtensionUI workbenchUI) {
        this.workbenchUI = workbenchUI;
    }

    /**
     * @return The workbench
     */
    public ACAQJsonExtensionUI getWorkbenchUI() {
        return workbenchUI;
    }

    /**
     * @return The extension
     */
    public ACAQJsonExtension getProject() {
        return workbenchUI.getProject();
    }
}
