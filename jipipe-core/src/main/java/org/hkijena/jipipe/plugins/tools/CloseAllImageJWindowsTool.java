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

import ij.WindowManager;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

public class CloseAllImageJWindowsTool extends JIPipeDesktopMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public CloseAllImageJWindowsTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Close all ImageJ windows");
        setToolTipText("Closes all open ImageJ-related windows.");
        setIcon(UIUtils.getIconFromResources("actions/close-tab.png"));
        addActionListener(e -> closeImageJWindows());
    }

    private void closeImageJWindows() {
        WindowManager.closeAllWindows();
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
