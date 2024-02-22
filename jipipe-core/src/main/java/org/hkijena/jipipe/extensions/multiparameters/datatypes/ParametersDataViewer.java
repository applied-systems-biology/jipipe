package org.hkijena.jipipe.extensions.multiparameters.datatypes;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ParametersDataViewer extends JIPipeWorkbenchPanel {

    private final DocumentTabPane tabPane = new DocumentTabPane(true, DocumentTabPane.TabPlacement.Top);
    private final EditorPane jsonViewer = new EditorPane();
    private final TableEditor tableViewer;
    private final ParameterPanel guiViewer;
    private ParametersData parametersData;

    /**
     * @param workbench the workbench
     */
    public ParametersDataViewer(JIPipeWorkbench workbench) {
        super(workbench);
        this.tableViewer = new TableEditor(workbench, new ResultsTableData());
        this.guiViewer = new ParameterPanel(getWorkbench(),
                new JIPipeDummyParameterCollection(),
                new MarkdownDocument("# Parameters\n\nThis panel displays the parameters. Editing the values has no effect."),
                ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION);
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
        getWorkbench().getContext().inject(jsonViewer);
        jsonViewer.setSyntaxEditingStyle("text/json");
        RTextScrollPane scrollPane = new RTextScrollPane(jsonViewer, true);
        scrollPane.setFoldIndicatorEnabled(true);
        tabPane.addTab("JSON view",
                UIUtils.getIconFromResources("actions/dialog-xml-editor.png"),
                scrollPane,
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeTableViewer() {
        tabPane.addTab("Table view",
                UIUtils.getIconFromResources("actions/table.png"),
                tableViewer,
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeGUIViewer() {
        tabPane.addTab("Graphical view",
                UIUtils.getIconFromResources("actions/followmouse.png"),
                guiViewer,
                DocumentTabPane.CloseMode.withoutCloseButton);

    }

    public DocumentTabPane getTabPane() {
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
