package org.hkijena.acaq5.extensions.tableoperations;

import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.extension.MenuExtension;
import org.hkijena.acaq5.ui.extension.MenuTarget;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableAnalyzerUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.table.DefaultTableModel;
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
        ACAQTableAnalyzerUI tableAnalyzerUI = new ACAQTableAnalyzerUI((ACAQProjectWorkbench) getWorkbench(), new DefaultTableModel());
        getWorkbench().getDocumentTabPane().addTab("Table", UIUtils.getIconFromResources("data-types/results-table.png"),
                tableAnalyzerUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }
}
