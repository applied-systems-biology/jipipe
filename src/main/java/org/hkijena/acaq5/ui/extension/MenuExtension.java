package org.hkijena.acaq5.ui.extension;

import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.ui.ACAQWorkbench;

import javax.swing.*;

/**
 * An extension for the extension builder or analysis tool menu.
 * Use {@link org.hkijena.acaq5.api.ACAQOrganization} to determine the target menu and any sub-menu
 */
public abstract class MenuExtension extends JMenuItem {

    private ACAQWorkbench workbench;

    /**
     * Creates a new instance
     * @param workbench workbench the extension is attached to
     */
    public MenuExtension(ACAQWorkbench workbench) {
        this.workbench = workbench;
    }

    public ACAQWorkbench getWorkbench() {
        return workbench;
    }

    public String getMenuPath() {
        return getClass().getAnnotation(ACAQOrganization.class).menuPath();
    }
}
