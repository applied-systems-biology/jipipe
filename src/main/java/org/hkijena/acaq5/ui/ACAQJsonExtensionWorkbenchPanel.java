package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQJsonExtension;

/**
 * UI panel that contains references to an {@link ACAQJsonExtension} UI
 */
public class ACAQJsonExtensionWorkbenchPanel extends ACAQWorkbenchPanel {

    private final ACAQJsonExtensionWorkbench workbenchUI;

    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQJsonExtensionWorkbenchPanel(ACAQJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        this.workbenchUI = workbenchUI;
    }

    /**
     * @return The workbench
     */
    public ACAQJsonExtensionWorkbench getExtensionWorkbenchUI() {
        return workbenchUI;
    }

    /**
     * @return The extension
     */
    public ACAQJsonExtension getProject() {
        return workbenchUI.getProject();
    }
}
