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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;

/**
 * A content panel with access to a {@link JIPipeWorkbench}
 */
public class JIPipeDesktopFlexContentWorkbenchPanel extends JIPipeDesktopFlexContentPanel implements JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench desktopWorkbench;

    public JIPipeDesktopFlexContentWorkbenchPanel(JIPipeDesktopWorkbench desktopWorkbench) {
        this(desktopWorkbench, NONE);
    }

    public JIPipeDesktopFlexContentWorkbenchPanel(JIPipeDesktopWorkbench desktopWorkbench, int flags) {
        super(flags);
        this.desktopWorkbench = desktopWorkbench;
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return desktopWorkbench;
    }
}
