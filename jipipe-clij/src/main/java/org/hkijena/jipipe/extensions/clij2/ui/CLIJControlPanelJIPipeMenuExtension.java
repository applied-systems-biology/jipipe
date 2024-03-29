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

package org.hkijena.jipipe.extensions.clij2.ui;

import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.UIUtils;

/**
 * Adds a menu item that allows to control CLIJ
 */
public class CLIJControlPanelJIPipeMenuExtension extends JIPipeMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public CLIJControlPanelJIPipeMenuExtension(JIPipeWorkbench workbench) {
        super(workbench);
        setText("CLIJ2 control panel");
        setIcon(UIUtils.getIconFromResources("apps/clij.png"));
        setToolTipText("Opens a control panel to setup CLIJ2");
        addActionListener(e -> openControlPanel());
    }

    private void openControlPanel() {
        for (DocumentTabPane.DocumentTab tab : getWorkbench().getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof CLIJControlPanel) {
                getWorkbench().getDocumentTabPane().switchToContent(tab.getContent());
                return;
            }
        }
        try {
            getWorkbench().getDocumentTabPane().addTab("CLIJ2 control panel",
                    UIUtils.getIconFromResources("apps/clij.png"),
                    new CLIJControlPanel(getWorkbench()),
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    false);
            getWorkbench().getDocumentTabPane().switchToLastTab();
        } catch (Exception e) {
            UIUtils.openErrorDialog(getWorkbench(), this, new JIPipeValidationRuntimeException(
                    e,
                    "Could not open CLIJ2 control panel!",
                    "This might indicate that your system is missing an essential library.",
                    "Please update your graphics card drivers. On Linux, CLIJ2 makes use of libOpencv.so. On Ubuntu, this library must be manually installed via the ocl-icd-opencl-dev package. Check if it is installed."
            ));
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Extensions";
    }
}
