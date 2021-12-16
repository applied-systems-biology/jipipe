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

package org.hkijena.jipipe.extensions.parameters;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.ParameterUITester;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New table" to the JIPipe menu
 */
public class ParameterTesterJIPipeMenuExtension extends JIPipeMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ParameterTesterJIPipeMenuExtension(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Show all parameter editors");
        setToolTipText("Opens a UI that shows all parameter types and their editors. This is useful for development.");
        setIcon(UIUtils.getIconFromResources("actions/bug.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ParameterUITester tester = new ParameterUITester(getWorkbench());
        getWorkbench().getDocumentTabPane().addTab("Registered JIPipe parameters", UIUtils.getIconFromResources("actions/bug.png"),
                tester, DocumentTabPane.CloseMode.withSilentCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
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
