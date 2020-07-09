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

package org.hkijena.pipelinej.extensions.parameters;

import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.components.DocumentTabPane;
import org.hkijena.pipelinej.ui.extension.MenuExtension;
import org.hkijena.pipelinej.ui.extension.MenuTarget;
import org.hkijena.pipelinej.utils.ParameterUITester;
import org.hkijena.pipelinej.utils.UIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New table" to the ACAQ5 menu
 */
@ACAQOrganization(menuExtensionTarget = MenuTarget.ProjectToolsMenu, menuPath = "Development")
public class ParameterTesterMenuExtension extends MenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ParameterTesterMenuExtension(ACAQWorkbench workbench) {
        super(workbench);
        setText("Show all parameter editors");
        setToolTipText("Opens a UI that shows all parameter types and their editors. This is useful for development.");
        setIcon(UIUtils.getIconFromResources("bug.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ParameterUITester tester = new ParameterUITester(getWorkbench());
        getWorkbench().getDocumentTabPane().addTab("Registered ACAQ5 parameters", UIUtils.getIconFromResources("bug.png"),
                tester, DocumentTabPane.CloseMode.withSilentCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }
}
