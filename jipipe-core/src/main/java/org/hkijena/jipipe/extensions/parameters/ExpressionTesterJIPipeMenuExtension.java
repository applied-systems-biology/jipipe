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

import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.ui.ExpressionTesterUI;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New table" to the JIPipe menu
 */
public class ExpressionTesterJIPipeMenuExtension extends JIPipeMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ExpressionTesterJIPipeMenuExtension(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Calculator");
        setToolTipText("Allows to evaluate mathematical and logical expressions.");
        setIcon(UIUtils.getIconFromResources("actions/calculator.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ExpressionTesterUI tester = new ExpressionTesterUI(getWorkbench());
        getWorkbench().getDocumentTabPane().addTab("Calculator", UIUtils.getIconFromResources("actions/calculator.png"),
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
