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

import ij.ImageJ;
import net.imagej.ui.swing.updater.ImageJUpdater;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

public class OpenImageJUpdateManagerTool extends JIPipeDesktopMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public OpenImageJUpdateManagerTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Open ImageJ Update Manager");
        setToolTipText("Opens the ImageJ update manager");
        setIcon(UIUtils.getIconFromResources("apps/imagej.png"));
        addActionListener(e -> showImageJ());
    }

    private void showImageJ() {
        ImageJUpdater updater = new ImageJUpdater();
        JIPipe.getInstance().getContext().inject(updater);
        updater.run();
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
