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

package org.hkijena.jipipe.ui.grapheditor.compartments.properties;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ribbon.*;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorMinimap;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeSingleCompartmentSelectionOverviewPanelUI extends JIPipeProjectWorkbenchPanel implements JIPipeCache.ModifiedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeSingleCompartmentSelectionPanelUI parentPanel;

    private final FormPanel formPanel = new FormPanel(FormPanel.WITH_SCROLLING);
    private final Ribbon ribbon = new Ribbon(2);
    private final JIPipeProjectCompartment compartment;
    private final JIPipeCompartmentOutput compartmentOutput;
    private final JIPipeGraphCanvasUI canvasUI;

    public JIPipeSingleCompartmentSelectionOverviewPanelUI(JIPipeSingleCompartmentSelectionPanelUI parentPanel) {
        super(parentPanel.getProjectWorkbench());
        this.parentPanel = parentPanel;
        this.canvasUI = parentPanel.getCanvas();
        this.compartment = parentPanel.getCompartment();
        this.compartmentOutput = compartment.getOutputNode();

        initialize();
        reload();

        getProject().getCache().getModifiedEventEmitter().subscribe(this);
        compartment.getParameterChangedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        splitPane.setTopComponent(new JIPipeGraphEditorMinimap(parentPanel.getGraphEditorUI()));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        splitPane.setBottomComponent(bottomPanel);

        bottomPanel.add(formPanel, BorderLayout.CENTER);
        bottomPanel.add(ribbon, BorderLayout.NORTH);
    }

    private void reload() {
        String currentTask = ribbon.getSelectedTask();
        ribbon.clear();
        initializeRibbon(ribbon);
        ribbon.rebuildRibbon();
        ribbon.selectTask(currentTask);

        formPanel.clear();
        initializeCompartment(formPanel);

        if (compartmentOutput != null) {
            Map<String, JIPipeDataTable> query = getProject().getCache().query(compartmentOutput, compartmentOutput.getUUIDInParentGraph(), new JIPipeProgressInfo());
            if (!query.isEmpty()) {
                initializeCache(formPanel, query);
            }
        }
        formPanel.addVerticalGlue();

        revalidate();
        repaint();
    }

    private void initializeCompartment(FormPanel formPanel) {
        FormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Compartments", UIUtils.getIconFromResources("actions/help-info.png"));
        groupHeader.addColumn(UIUtils.createButton("Edit", UIUtils.getIconFromResources("actions/edit.png"), this::editCompartmentContents));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("Graph compartments organize the pipeline into units, which is helpful for larger workflows. " +
                "To edit the steps that are executed in a compartment, double-click the compartment in the interface or click the 'Edit' button.", false));
    }

    private void initializeRibbon(Ribbon ribbon) {
        initializeRibbonNodeTask(ribbon);
        initializeRibbonEditTask(ribbon);
    }

    private void initializeRibbonEditTask(Ribbon ribbon) {
        Ribbon.Task editTask = ribbon.addTask("Edit");
        Ribbon.Band generalBand = editTask.addBand("General");
        generalBand.add(new LargeToggleButtonAction("Lock", "If enabled, the node cannot be deleted or moved anymore.",
                UIUtils.getIcon32FromResources("actions/lock.png"),
                compartment.isUiLocked(),
                (button) -> {
                    compartment.setParameter("jipipe:node:ui-locked", button.isSelected());
                }));
        generalBand.add(new LargeButtonAction("Delete compartment", "Deletes the node", UIUtils.getIcon32FromResources("actions/trash.png"), this::deleteCompartment));
    }

    private void deleteCompartment() {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvasUI.getWorkbench()))
            return;
        Set<JIPipeGraphNode> selection = Collections.singleton(compartment);
        if (!GraphEditorUISettings.getInstance().isAskOnDeleteCompartment() || JOptionPane.showConfirmDialog(canvasUI.getWorkbench().getWindow(),
                "Do you really want to remove the following compartment: " +
                        selection.stream().filter(node -> !node.isUiLocked()).map(JIPipeGraphNode::getName).collect(Collectors.joining(", ")), "Delete compartments",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (JIPipeGraphNode node : ImmutableList.copyOf(selection)) {
                if (node.isUiLocked())
                    continue;
                if (node instanceof JIPipeProjectCompartment) {
                    JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) node;
                    if (canvasUI.getHistoryJournal() != null) {
                        canvasUI.getHistoryJournal().snapshotBeforeRemoveCompartment(compartment);
                    }
                    compartment.getRuntimeProject().removeCompartment(compartment);
                } else {
                    canvasUI.getGraph().removeNode(node, true);
                }
            }
        }
    }

    private void initializeRibbonNodeTask(Ribbon ribbon) {
        Ribbon.Task nodeTask = ribbon.addTask("Node");
        if (compartmentOutput != null) {
            Ribbon.Band workloadBand = nodeTask.addBand("Workload");
            List<JMenuItem> runMenuItems = new ArrayList<>();
            for (NodeUIContextAction entry : JIPipeGraphNodeUI.RUN_NODE_CONTEXT_MENU_ENTRIES) {
                if (entry == null)
                    runMenuItems.add(null);
                else {
                    JMenuItem item = new JMenuItem(entry.getName(), entry.getIcon());
                    item.setToolTipText(entry.getDescription());
                    item.setAccelerator(entry.getKeyboardShortcut());
                    item.addActionListener(e -> {
                        JIPipeGraphNodeUI nodeUI = canvasUI.getNodeUIs().get(compartment);
                        if (entry.matches(Collections.singleton(nodeUI))) {
                            entry.run(canvasUI, Collections.singleton(nodeUI));
                        } else {
                            JOptionPane.showMessageDialog(getWorkbench().getWindow(),
                                    "Could not run this operation",
                                    entry.getName(),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    runMenuItems.add(item);
                }
            }
            workloadBand.add(new LargeButtonAction("Run compartment output", "Runs the output node of the current compartment", UIUtils.getIcon32FromResources("actions/play.png"),
                    runMenuItems.toArray(new JMenuItem[0])));
        }

        Ribbon.Band groupBand = nodeTask.addBand("Compartment");
        groupBand.add(new LargeButtonAction("Edit contents", "Edits the contents of the compartment", UIUtils.getIcon32FromResources("actions/edit.png"), this::editCompartmentContents));
    }

    private void editCompartmentContents() {
        getProjectWorkbench().getOrOpenPipelineEditorTab(compartment, true);
    }

    private void initializeCache(FormPanel formPanel, Map<String, JIPipeDataTable> query) {
        FormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Results available", UIUtils.getIconFromResources("actions/database.png"));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("Previously generated results are stored in the memory cache. Click the 'Show results' button to review the results.", false));
        groupHeader.addColumn(UIUtils.createButton("Show results", UIUtils.getIconFromResources("actions/open-in-new-window.png"), this::openCacheBrowser));

        FormPanel ioTable = new FormPanel(FormPanel.NONE);
        for (JIPipeOutputDataSlot outputSlot : compartmentOutput.getOutputSlots()) {
            String infoString;
            JIPipeDataTable cachedData = query.getOrDefault(outputSlot.getName(), null);
            if (cachedData != null) {
                infoString = cachedData.getRowCount() > 1 ? cachedData.getRowCount() + " items" : "1 item";
            }
            else {
                infoString = "0 items";
            }
            ioTable.addToForm(UIUtils.makeBorderlessReadonlyTextPane(infoString, false),
                    new JLabel("Out: " + StringUtils.orElse(outputSlot.getInfo().getCustomName(), outputSlot.getName()), JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()), JLabel.LEFT));
        }

        formPanel.addWideToForm(ioTable);
    }

    private void openCacheBrowser() {
        parentPanel.getTabbedPane().selectSingletonTab("CACHE_BROWSER");
    }


    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        if (!isDisplayable()) {
            getProject().getCache().getModifiedEventEmitter().unsubscribe(this);
            return;
        }
        SwingUtilities.invokeLater(this::reload);
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (!isDisplayable()) {
            compartment.getParameterChangedEventEmitter().unsubscribe(this);
            return;
        }
        if ("jipipe:algorithm:enabled".equals(event.getKey()) || "jipipe:algorithm:pass-through".equals(event.getKey()) || "jipipe:node:ui-locked".equals(event.getKey())) {
            SwingUtilities.invokeLater(this::reload);
        }
    }
}
