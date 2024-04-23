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

package org.hkijena.jipipe.desktop.app.plugins;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeDesktopManagePluginsButton extends JButton implements JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench workbench;

    public JIPipeDesktopManagePluginsButton(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        initialize();
    }

    private void initialize() {
        UIUtils.setStandardButtonBorder(this);
        setText("Plugins");
        setIcon(UIUtils.getIconFromResources("actions/preferences-plugin.png"));
    }

    @Override
    public JIPipeDesktopWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }
}
