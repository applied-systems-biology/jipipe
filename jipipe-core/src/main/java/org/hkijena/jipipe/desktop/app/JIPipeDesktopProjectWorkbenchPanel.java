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

package org.hkijena.jipipe.desktop.app;

import org.hkijena.jipipe.api.JIPipeWorkbench;

import javax.swing.*;

/**
 * A {@link JPanel} that implements {@link JIPipeDesktopProjectWorkbenchAccess}
 */
public class JIPipeDesktopProjectWorkbenchPanel extends JPanel implements JIPipeDesktopProjectWorkbenchAccess {
    private final JIPipeDesktopProjectWorkbench workbench;

    public JIPipeDesktopProjectWorkbenchPanel(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
