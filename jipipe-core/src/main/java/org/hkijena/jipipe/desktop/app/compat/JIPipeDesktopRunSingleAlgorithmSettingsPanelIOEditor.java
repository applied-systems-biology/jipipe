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

package org.hkijena.jipipe.desktop.app.compat;

import org.hkijena.jipipe.api.compat.ImageJDataExportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.history.JIPipeDummyGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.util.Map;

public class JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditor extends AbstractJIPipeDesktopGraphEditorUI implements JIPipeGraphNode.NodeSlotsChangedEventListener {

    private final JIPipeDesktopFormPanel inputsPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeDesktopFormPanel outputsPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeDesktopRunSingleAlgorithmSettingsPanel settingsPanel;

    public JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditor(JIPipeDesktopRunSingleAlgorithmSettingsPanel settingsPanel) {
        super(settingsPanel.getDesktopWorkbench(),
                createGraph(settingsPanel.getNode()),
                null,
                new JIPipeDummyGraphHistoryJournal(),
                createSettings()
        );
        this.settingsPanel = settingsPanel;
        initialize();
        reloadPropertyPanel();
        settingsPanel.getRun().getNodeSlotsChangedEventEmitter().subscribeWeak(this);
    }

    private static JIPipeGraph createGraph(JIPipeGraphNode node) {
        JIPipeGraph graph = new JIPipeGraph();
        graph.insertNode(node);
        return graph;
    }

    private static JIPipeGraphEditorUIApplicationSettings createSettings() {
        return new JIPipeGraphEditorUIApplicationSettings();
    }

    @Override
    protected void restoreDockStateFromSettings() {

    }

    @Override
    protected void saveDockStateToSettings() {

    }

    private void initialize() {
        JIPipeDesktopTabPane propertyPanel = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
        propertyPanel.addTab("Inputs",
                UIUtils.getIconFromResources("data-types/slot.png"),
                inputsPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        propertyPanel.addTab("Outputs",
                UIUtils.getIconFromResources("data-types/slot.png"),
                outputsPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        getDockPanel().addDockPanel("Inputs",
                "Inputs",
                UIUtils.getIconFromResources("data-types/slot.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                false,
                0, inputsPanel);
        getDockPanel().addDockPanel("Outputs",
                "Outputs",
                UIUtils.getIconFromResources("data-types/slot.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                false,
                0, outputsPanel);
    }

    @Override
    protected StringAndStringPairParameter.List getDockStateTemplates() {
        return null;
    }

    @Override
    protected void restoreDefaultDockState() {

    }

    @Override
    public void beforeOpenContextMenu(JPopupMenu menu) {

    }

    public JIPipeDesktopRunSingleAlgorithmSettingsPanel getSettingsPanel() {
        return settingsPanel;
    }

    @Override
    public void onNodeSlotsChanged(JIPipeGraphNode.NodeSlotsChangedEvent event) {
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
                JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditorInputUI ui = new JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditorInputUI(this, entry.getKey());
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
                JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditorOutputUI ui = new JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditorOutputUI(this, entry.getKey());
                outputsPanel.addWideToForm(ui, null);
            }
        }
        outputsPanel.addVerticalGlue();
    }

}
