/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.api;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

import javax.swing.*;

/**
 * An extension for the extension builder or analysis tool menu.
 * Use {@link ConfigureJIPipeNode} to determine the target menu and any sub-menu
 */
public abstract class JIPipeDesktopMenuExtension extends JMenuItem {

    private final JIPipeDesktopWorkbench desktopWorkbench;

    /**
     * Creates a new instance
     *
     * @param desktopWorkbench workbench the extension is attached to
     */
    public JIPipeDesktopMenuExtension(JIPipeDesktopWorkbench desktopWorkbench) {
        this.desktopWorkbench = desktopWorkbench;
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }

    public abstract JIPipeMenuExtensionTarget getMenuTarget();

    public abstract String getMenuPath();
}
