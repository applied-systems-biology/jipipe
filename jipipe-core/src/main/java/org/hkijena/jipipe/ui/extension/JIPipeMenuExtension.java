/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.extension;

import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;

/**
 * An extension for the extension builder or analysis tool menu.
 * Use {@link JIPipeNode} to determine the target menu and any sub-menu
 */
public abstract class JIPipeMenuExtension extends JMenuItem {

    private final JIPipeWorkbench workbench;

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public JIPipeMenuExtension(JIPipeWorkbench workbench) {
        this.workbench = workbench;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public abstract JIPipeMenuExtensionTarget getMenuTarget();

    public abstract String getMenuPath();
}
