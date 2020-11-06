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

import ij.ImageJ;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.utils.UIUtils;

@JIPipeOrganization(menuExtensionTarget = MenuTarget.ProjectToolsMenu)
public class OpenImageJTool extends MenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public OpenImageJTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Open ImageJ");
        setToolTipText("Opens ImageJ (useful if you started JIPipe outside of ImageJ).");
        setIcon(UIUtils.getIconFromResources("apps/imagej.png"));
        addActionListener(e -> showImageJ());
    }

    private void showImageJ() {
        final ImageJ ij = new ImageJ(ImageJ.EMBEDDED);
        ij.setVisible(true);
    }
}
