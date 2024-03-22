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

package org.hkijena.jipipe.extensions.multiparameters.datatypes;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.extensions.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ParametersDataViewer extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
    private final EditorPane jsonViewer = new EditorPane();
    private final JIPipeDesktopTableEditor tableViewer;
    private final JIPipeDesktopParameterPanel guiViewer;
    private ParametersData parametersData;

    /**
     * @param workbench the workbench
     */
    public ParametersDataViewer(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        this.tableViewer = new JIPipeDesktopTableEditor(workbench, new ResultsTableData());
        this.guiViewer = new JIPipeDesktopParameterPanel(getDesktopWorkbench(),
                new JIPipeDummyParameterCollection(),
                new MarkdownText("# Parameters\n\nThis panel displays the parameters. Editing the values has no effect."),
                JIPipeDesktopParameterPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.WITH_DOCUMENTATION);
        initialize();
    }

    private void updateData() {
        if (parametersData != null) {
            // GUI
            JIPipeDynamicParameterCollection asGUI = new JIPipeDynamicParameterCollection();
            for (Map.Entry<String, Object> entry : ((ParametersData) parametersData.duplicate(new JIPipeProgressInfo())).getParameterData().entrySet()) {
                if (entry.getValue() == null)
                    continue;
                asGUI.addParameter(entry.getKey(), entry.getValue().getClass(), entry.getKey(), "");
                asGUI.getParameter(entry.getKey()).set(entry.getValue());
            }
            guiViewer.setDisplayedParameters(asGUI);

            // Table
            ResultsTableData asTable = new ResultsTableData();
            asTable.addRow(parametersData.getParameterData());
            tableViewer.setTableModel(asTable);

            // Json
            String asJson = JsonUtils.toPrettyJsonString(parametersData);
            jsonViewer.setText(asJson);
        } else {
            guiViewer.setDisplayedParameters(new JIPipeDummyParameterCollection());
            tableViewer.setTableModel(new ResultsTableData());
            jsonViewer.setText("");
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeGUIViewer();
        initializeTableViewer();
        initializeJsonViewer();
        add(tabPane, BorderLayout.CENTER);
    }

    private void initializeJsonViewer() {
        UIUtils.applyThemeToCodeEditor(jsonViewer);
        jsonViewer.setBackground(UIManager.getColor("TextArea.background"));
        jsonViewer.setHighlightCurrentLine(false);
        jsonViewer.setTabSize(4);
        getDesktopWorkbench().getContext().inject(jsonViewer);
        jsonViewer.setSyntaxEditingStyle("text/json");
        RTextScrollPane scrollPane = new RTextScrollPane(jsonViewer, true);
        scrollPane.setFoldIndicatorEnabled(true);
        tabPane.addTab("JSON view",
                UIUtils.getIconFromResources("actions/dialog-xml-editor.png"),
                scrollPane,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeTableViewer() {
        tabPane.addTab("Table view",
                UIUtils.getIconFromResources("actions/table.png"),
                tableViewer,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeGUIViewer() {
        tabPane.addTab("Graphical view",
                UIUtils.getIconFromResources("actions/followmouse.png"),
                guiViewer,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

    }

    public JIPipeDesktopTabPane getTabPane() {
        return tabPane;
    }

    public ParametersData getParametersData() {
        return parametersData;
    }

    public void setParametersData(ParametersData parametersData) {
        this.parametersData = parametersData;
        updateData();
    }
}
