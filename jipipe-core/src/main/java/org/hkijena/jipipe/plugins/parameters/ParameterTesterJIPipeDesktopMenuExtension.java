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

package org.hkijena.jipipe.plugins.parameters;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.ParameterUITester;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New table" to the JIPipe menu
 */
public class ParameterTesterJIPipeDesktopMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ParameterTesterJIPipeDesktopMenuExtension(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Show all parameter editors");
        setToolTipText("Opens a UI that shows all parameter types and their editors. This is useful for development.");
        setIcon(UIUtils.getIconFromResources("actions/bug.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ParameterUITester tester = new ParameterUITester(getDesktopWorkbench());
        getDesktopWorkbench().getDocumentTabPane().addTab("Registered JIPipe parameters", UIUtils.getIconFromResources("actions/bug.png"),
                tester, JIPipeDesktopTabPane.CloseMode.withSilentCloseButton, true);
        getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
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
