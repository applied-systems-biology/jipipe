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

package org.hkijena.pipelinej.extensions.tables;

import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.components.DocumentTabPane;
import org.hkijena.pipelinej.ui.extension.MenuExtension;
import org.hkijena.pipelinej.ui.extension.MenuTarget;
import org.hkijena.pipelinej.ui.tableanalyzer.ACAQTableEditor;
import org.hkijena.pipelinej.utils.UIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New table" to the ACAQ5 menu
 */
@ACAQOrganization(menuExtensionTarget = MenuTarget.ProjectMainMenu)
public class NewTableMenuExtension extends MenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public NewTableMenuExtension(ACAQWorkbench workbench) {
        super(workbench);
        setText("New table");
        setIcon(UIUtils.getIconFromResources("data-types/results-table.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ACAQTableEditor tableAnalyzerUI = new ACAQTableEditor((ACAQProjectWorkbench) getWorkbench(), new ResultsTableData());
        getWorkbench().getDocumentTabPane().addTab("Table", UIUtils.getIconFromResources("data-types/results-table.png"),
                tableAnalyzerUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }
}
