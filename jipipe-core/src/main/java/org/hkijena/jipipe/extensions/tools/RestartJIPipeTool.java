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

package org.hkijena.jipipe.extensions.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.ui.swing.console.SwingConsolePane;

import javax.swing.*;

public class RestartJIPipeTool extends JIPipeMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public RestartJIPipeTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Restart JIPipe");
        setIcon(UIUtils.getIconFromResources("apps/jipipe.png"));
        addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(workbench.getWindow(), "Do you really want to restart JIPipe? You will lose all unsaved changes.", "Restart JIPipe", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                JIPipe.restartGUI();
            }
        });
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Development";
    }
}
