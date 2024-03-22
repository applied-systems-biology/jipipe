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

package org.hkijena.jipipe.extensions.tables;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Adds an entry "New table" to the JIPipe menu
 */
public class NewTableJIPipeDesktopMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public NewTableJIPipeDesktopMenuExtension(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("New table");
        setIcon(UIUtils.getIconFromResources("data-types/results-table.png"));
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JIPipeDesktopTableEditor tableAnalyzerUI = new JIPipeDesktopTableEditor((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), new ResultsTableData());
        getDesktopWorkbench().getDocumentTabPane().addTab("Table", UIUtils.getIconFromResources("data-types/results-table.png"),
                tableAnalyzerUI, JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton, true);
        getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectMainMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }
}
