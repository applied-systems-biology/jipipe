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

package org.hkijena.jipipe.plugins.tools;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.ui.swing.console.SwingConsolePane;

public class OpenImageJConsoleTool extends JIPipeDesktopMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public OpenImageJConsoleTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Open console");
        setToolTipText("Opens the stdout/stderr console.");
        setIcon(UIUtils.getIconFromResources("actions/akonadiconsole.png"));
        addActionListener(e -> showImageJ());
    }

    private void showImageJ() {
        UIService uiService = getDesktopWorkbench().getContext().getService(UIService.class);
        final UserInterface ui = uiService.getDefaultUI();
        if (ui != null && ui.getConsolePane() != null) {
            ui.show();
        } else {
            SwingConsolePane swingConsolePane = new SwingConsolePane(getDesktopWorkbench().getContext());
            getDesktopWorkbench().getDocumentTabPane().addTab("Console",
                    UIUtils.getIconFromResources("actions/akonadiconsole.png"),
                    swingConsolePane.getComponent(),
                    JIPipeDesktopTabPane.CloseMode.withSilentCloseButton);
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "ImageJ";
    }
}
