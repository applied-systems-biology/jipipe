package org.hkijena.jipipe.ui.compat;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.history.JIPipeDummyGraphHistoryJournal;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.UUID;

public class RunSingleAlgorithmSettingsPanelIOEditor extends JIPipeGraphEditorUI {

    private final FormPanel propertyPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private final RunSingleAlgorithmSettingsPanel settingsPanel;

    public RunSingleAlgorithmSettingsPanelIOEditor(RunSingleAlgorithmSettingsPanel settingsPanel) {
        super(settingsPanel.getWorkbench(), createGraph(settingsPanel.getNode()), null, new JIPipeDummyGraphHistoryJournal(), createSettings());
        this.settingsPanel = settingsPanel;
        setPropertyPanel(propertyPanel);
        reloadPropertyPanel();
        settingsPanel.getNode().getSlotConfiguration().getEventBus().register(this);
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

    @Subscribe
    public void onSlotsChanged(JIPipeSlotConfiguration.SlotsChangedEvent event) {
        reloadPropertyPanel();
    }

    private void reloadPropertyPanel() {
        propertyPanel.clear();
        if(!settingsPanel.getNode().getInputSlots().isEmpty()) {
            FormPanel.GroupHeaderPanel groupHeader = propertyPanel.addGroupHeader("Inputs", UIUtils.getIconFromResources("data-types/slot.png"));
            groupHeader.setDescription("Please use the following items to assign inputs to the node:");
        }
        propertyPanel.addVerticalGlue();
    }

}
