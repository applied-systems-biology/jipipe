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

package org.hkijena.jipipe.plugins.ilastik;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

public class RunIlastikDesktopMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public RunIlastikDesktopMenuExtension(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Run Ilastik");
        setIcon(IlastikPlugin.RESOURCES.getIconFromResources("ilastik.png"));
        setToolTipText("Starts a new instance of Ilastik.");
        addActionListener(this);
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        IlastikPlugin.launchIlastik(getDesktopWorkbench(), Collections.emptyList());
    }
}
