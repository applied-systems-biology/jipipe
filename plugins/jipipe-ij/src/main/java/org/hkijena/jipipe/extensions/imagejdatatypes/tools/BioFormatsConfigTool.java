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

package org.hkijena.jipipe.extensions.imagejdatatypes.tools;

import loci.plugins.config.ConfigWindow;
import loci.plugins.util.WindowTools;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.UIUtils;

public class BioFormatsConfigTool extends JIPipeMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public BioFormatsConfigTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Configure Bio-Formats");
        setToolTipText("Opens the Bio-Formats configuration tool.");
        setIcon(UIUtils.getIconFromResources("apps/bioformats.png"));
        addActionListener(e -> showConfigWindow());
    }

    private void showConfigWindow() {
        ConfigWindow cw = new ConfigWindow();
        WindowTools.placeWindow(cw);
        cw.setVisible(true);
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }
}
