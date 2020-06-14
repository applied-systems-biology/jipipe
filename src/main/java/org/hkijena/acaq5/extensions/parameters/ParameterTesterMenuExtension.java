package org.hkijena.acaq5.extensions.parameters;

import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.extension.MenuExtension;
import org.hkijena.acaq5.ui.extension.MenuTarget;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableEditor;
import org.hkijena.acaq5.utils.ParameterUITester;
import org.hkijena.acaq5.utils.UIUtils;

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
