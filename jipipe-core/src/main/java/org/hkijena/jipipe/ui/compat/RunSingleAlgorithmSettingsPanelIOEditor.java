package org.hkijena.jipipe.ui.compat;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.compat.ImageJDataExportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.history.JIPipeDummyGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.Map;

public class RunSingleAlgorithmSettingsPanelIOEditor extends JIPipeGraphEditorUI {

    private final FormPanel inputsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final FormPanel outputsPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final RunSingleAlgorithmSettingsPanel settingsPanel;

    public RunSingleAlgorithmSettingsPanelIOEditor(RunSingleAlgorithmSettingsPanel settingsPanel) {
        super(settingsPanel.getWorkbench(),
                createGraph(settingsPanel.getNode()),
                null,
                new JIPipeDummyGraphHistoryJournal(),
                createSettings(),
                JIPipeGraphEditorUI.FLAGS_SPLIT_PANE_VERTICAL | JIPipeGraphEditorUI.FLAGS_SPLIT_PANE_SWITCH_CONTENT);
        this.settingsPanel = settingsPanel;
        initialize();
        reloadPropertyPanel();
        settingsPanel.getRun().getEventBus().register(this);
    }

    private static JIPipeGraph createGraph(JIPipeGraphNode node) {
        JIPipeGraph graph = new JIPipeGraph();
        graph.insertNode(node);
        return graph;
    }

    private static GraphEditorUISettings createSettings() {
        GraphEditorUISettings settings = new GraphEditorUISettings();
        settings.getSearchSettings().setEnableSearch(false);
        return settings;
    }

    private void initialize() {
        DocumentTabPane propertyPanel = new DocumentTabPane(true);
        propertyPanel.addTab("Inputs",
                UIUtils.getIconFromResources("data-types/slot.png"),
                inputsPanel,
                DocumentTabPane.CloseMode.withoutCloseButton);
        propertyPanel.addTab("Outputs",
                UIUtils.getIconFromResources("data-types/slot.png"),
                outputsPanel,
                DocumentTabPane.CloseMode.withoutCloseButton);
        setPropertyPanel(propertyPanel, true);
    }

    public RunSingleAlgorithmSettingsPanel getSettingsPanel() {
        return settingsPanel;
    }

    @Subscribe
    public void onSlotsChanged(JIPipeGraph.NodeSlotsChangedEvent event) {
        reloadPropertyPanel();
    }

    private void reloadPropertyPanel() {
        inputsPanel.clear();
        outputsPanel.clear();
        if (!settingsPanel.getNode().getInputSlots().isEmpty()) {
//            FormPanel.GroupHeaderPanel groupHeader = inputsPanel.addGroupHeader("Inputs", UIUtils.getIconFromResources("data-types/slot.png"));
//            groupHeader.setDescription("Please use the following items to assign inputs to the node. " +
//                    "Please note that JIPipe provides multiple options how JIPipe can acquire data from ImageJ. " +
//                    "To change the importer, click the 'Edit' button.");
            for (Map.Entry<String, ImageJDataImportOperation> entry : settingsPanel.getRun().getInputSlotImporters().entrySet()) {
//                JLabel label = new JLabel(entry.getKey());
//                label.setIcon(JIPipe.getDataTypes().getIconFor(settingsPanel.getNode().getInputSlot(entry.getKey()).getAcceptedDataType()));
//                ImageJDataImporterUI ui = JIPipe.getImageJAdapters().createUIForImportOperation(getWorkbench(), entry.getValue());
//                propertyPanel.addToForm(ui, label, null);
                RunSingleAlgorithmSettingsPanelIOEditorInputUI ui = new RunSingleAlgorithmSettingsPanelIOEditorInputUI(this, entry.getKey());
                inputsPanel.addWideToForm(ui, null);
            }
        }
        inputsPanel.addVerticalGlue();
        if (!settingsPanel.getNode().getOutputSlots().isEmpty()) {
//            FormPanel.GroupHeaderPanel groupHeader = inputsPanel.addGroupHeader("Outputs", UIUtils.getIconFromResources("data-types/slot.png"));
//            groupHeader.setDescription("The following items refer to the generated outputs. " +
//                    "Please note that JIPipe can export results back into ImageJ in multiple ways. " +
//                    "To change the exporter, click the 'Edit' button.");
            for (Map.Entry<String, ImageJDataExportOperation> entry : settingsPanel.getRun().getOutputSlotExporters().entrySet()) {
                RunSingleAlgorithmSettingsPanelIOEditorOutputUI ui = new RunSingleAlgorithmSettingsPanelIOEditorOutputUI(this, entry.getKey());
                outputsPanel.addWideToForm(ui, null);
            }
        }
        outputsPanel.addVerticalGlue();
    }

}
