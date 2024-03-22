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

package org.hkijena.jipipe.desktop.jsonextensionbuilder;

import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

/**
 * UI panel that contains references to an {@link JIPipeJsonPlugin} UI
 */
public class JIPipeDesktopJsonExtensionWorkbenchPanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopJsonExtensionWorkbench workbenchUI;

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeDesktopJsonExtensionWorkbenchPanel(JIPipeDesktopJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        this.workbenchUI = workbenchUI;
    }

    /**
     * @return The workbench
     */
    public JIPipeDesktopJsonExtensionWorkbench getExtensionWorkbenchUI() {
        return workbenchUI;
    }

    /**
     * @return The extension
     */
    public JIPipeJsonPlugin getPluginProject() {
        return workbenchUI.getPluginProject();
    }
}
