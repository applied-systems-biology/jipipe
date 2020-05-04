package org.hkijena.acaq5.extensions.plots;

import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.extension.MenuExtension;
import org.hkijena.acaq5.ui.extension.MenuTarget;
import org.hkijena.acaq5.ui.plotbuilder.ACAQPlotBuilderUI;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableAnalyzerUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New plot" to the ACAQ5 menu
 */
@ACAQOrganization(menuExtensionTarget = MenuTarget.ProjectMainMenu)
public class NewPlotMenuExtension extends MenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public NewPlotMenuExtension(ACAQWorkbench workbench) {
        super(workbench);
        setText("New plot");
        setIcon(UIUtils.getIconFromResources("data-types/data-type-plot.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ACAQPlotBuilderUI plotBuilderUI = new ACAQPlotBuilderUI(getWorkbench());
        getWorkbench().getDocumentTabPane().addTab("Plot", UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }
}
