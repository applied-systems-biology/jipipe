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

package org.hkijena.jipipe.plugins.tables.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData}
 */
public class ResultsTableDataDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    public ResultsTableDataDesktopParameterEditorUI(InitializationParameters parameters) {
       super(parameters);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editButton = new JButton("Edit table", UIUtils.getIconFromResources("actions/edit.png"));
        editButton.addActionListener(e -> editParameters());
        add(editButton, BorderLayout.CENTER);
    }

    private void editParameters() {
        ResultsTableData table = getParameter(ResultsTableData.class);
        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), table, "Edit table");
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {

    }
}
