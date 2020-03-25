package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQProject;

import javax.swing.*;

public class ACAQJsonExtensionUIPanel extends JPanel {

    private final ACAQJsonExtensionUI workbenchUI;

    public ACAQJsonExtensionUIPanel(ACAQJsonExtensionUI workbenchUI) {
        this.workbenchUI = workbenchUI;
    }

    public ACAQJsonExtensionUI getWorkbenchUI() {
        return workbenchUI;
    }

    public ACAQJsonExtension getProject() {
        return workbenchUI.getProject();
    }
}
