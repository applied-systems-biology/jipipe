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

package org.hkijena.jipipe.ui;

import org.hkijena.jipipe.JIPipeJsonExtension;

/**
 * UI panel that contains references to an {@link JIPipeJsonExtension} UI
 */
public class JIPipeJsonExtensionWorkbenchPanel extends JIPipeWorkbenchPanel {

    private final JIPipeJsonExtensionWorkbench workbenchUI;

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeJsonExtensionWorkbenchPanel(JIPipeJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        this.workbenchUI = workbenchUI;
    }

    /**
     * @return The workbench
     */
    public JIPipeJsonExtensionWorkbench getExtensionWorkbenchUI() {
        return workbenchUI;
    }

    /**
     * @return The extension
     */
    public JIPipeJsonExtension getProject() {
        return workbenchUI.getProject();
    }
}
