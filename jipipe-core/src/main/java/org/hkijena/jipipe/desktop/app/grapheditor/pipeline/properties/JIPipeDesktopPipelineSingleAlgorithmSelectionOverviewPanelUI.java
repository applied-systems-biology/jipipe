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

package org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDesktopAlgorithmCompendiumUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.groups.JIPipeDesktopNodeGroupUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.*;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.extensions.nodeexamples.JIPipeNodeExamplePickerDialog;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.*;

public class JIPipeDesktopPipelineSingleAlgorithmSelectionOverviewPanelUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener, JIPipeCache.ModifiedEventListener {

    private final JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI parentPanel;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeGraphNode node;
    private final JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon(2);

    public JIPipeDesktopPipelineSingleAlgorithmSelectionOverviewPanelUI(JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI parentPanel) {
        super(parentPanel.getDesktopProjectWorkbench());
        this.parentPanel = parentPanel;
        this.canvasUI = parentPanel.getCanvas();
        this.node = parentPanel.getNode();
        initialize();
        reload();

        node.getParameterChangedEventEmitter().subscribe(this);
        getProject().getCache().getModifiedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        splitPane.setTopComponent(new JIPipeDesktopGraphEditorMinimap(parentPanel.getGraphEditorUI()));

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
        initializeDocumentation(formPanel);
        if (node instanceof JIPipeNodeGroup) {
            initializeNodeGroup(formPanel);
        }
        initializeParameters(formPanel);
        if (node instanceof JIPipeAlgorithm) {
            Map<String, JIPipeDataTable> query = getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
            if (!query.isEmpty()) {
                initializeCache(formPanel, query);
            }
        }
        formPanel.addVerticalGlue();

        revalidate();
        repaint();
    }

    private void initializeParameters(JIPipeDesktopFormPanel formPanel) {
//        JIPipeGraphNode copyNode = node.getInfo().newInstance();
//        GraphNodeParameterReferenceGroupCollection
    }

    private void initializeCache(JIPipeDesktopFormPanel formPanel, Map<String, JIPipeDataTable> query) {
        JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Results available", UIUtils.getIconFromResources("actions/database.png"));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("Previously generated results are stored in the memory cache. Click the 'Show results' button to review the results.", false));
        groupHeader.addColumn(UIUtils.createButton("Show results", UIUtils.getIconFromResources("actions/open-in-new-window.png"), this::openCacheBrowser));

        JIPipeDesktopFormPanel ioTable = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
        for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
            JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(outputSlot.getAcceptedDataType());
            String infoString;
            JIPipeDataTable cachedData = query.getOrDefault(outputSlot.getName(), null);
            if (cachedData != null) {
                infoString = cachedData.getRowCount() > 1 ? cachedData.getRowCount() + " items" : "1 item";
            } else {
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

    private void initializeNodeGroup(JIPipeDesktopFormPanel formPanel) {
        JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Group contents", UIUtils.getIconFromResources("actions/help-info.png"));
        groupHeader.addColumn(UIUtils.createButton("Edit", UIUtils.getIconFromResources("actions/edit.png"), this::editNodeGroupContents));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("This node executes the content of a sub-pipeline. You can modify it by clicking the 'Edit' button.", false));

    }

    private void initializeRibbon(JIPipeDesktopRibbon ribbon) {
        initializeRibbonNodeTask(ribbon);
        initializeRibbonEditTask(ribbon);
    }

    private void initializeRibbonEditTask(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task editTask = ribbon.addTask("Edit");
        JIPipeDesktopRibbon.Band generalBand = editTask.addBand("General");
        generalBand.add(new JIPipeDesktopLargeToggleButtonRibbonAction("Lock", "If enabled, the node cannot be deleted or moved anymore.",
                UIUtils.getIcon32FromResources("actions/lock.png"),
                node.isUiLocked(),
                (button) -> {
                    node.setParameter("jipipe:node:ui-locked", button.isSelected());
                }));
        JIPipeDesktopRibbon.Band deleteBand = editTask.addBand("Delete");
        deleteBand.add(new JIPipeDesktopLargeButtonRibbonAction("Isolate node", "Removes all connections of this node", UIUtils.getIcon32FromResources("actions/network-disconnect.png"), this::isolateNode));
        if (!node.isUiLocked()) {
            deleteBand.add(new JIPipeDesktopLargeButtonRibbonAction("Delete node", "Deletes the node", UIUtils.getIcon32FromResources("actions/trash.png"), this::deleteNode));
        }
//        Ribbon.Band miscBand = editTask.addBand("Misc");
    }

    private void isolateNode() {
        if (JOptionPane.showConfirmDialog(canvasUI.getDesktopWorkbench().getWindow(),
                "Do you really want to isolate the node?", "Isolate node",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Set<JIPipeGraphNode> nodes = Collections.singleton(node);
            if (canvasUI.getHistoryJournal() != null) {
                UUID compartment = nodes.stream().map(JIPipeGraphNode::getUUIDInParentGraph).findFirst().orElse(null);
                canvasUI.getHistoryJournal().snapshot("Isolate node", "Isolated node", compartment, UIUtils.getIconFromResources("actions/network-disconnect.png"));
            }
            Set<JIPipeDataSlot> slots = new HashSet<>();
            for (JIPipeGraphNode ui : nodes) {
                slots.addAll(ui.getInputSlots());
                slots.addAll(ui.getOutputSlots());
            }
            for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : canvasUI.getGraph().getSlotEdges()) {
                boolean isSource = slots.contains(edge.getKey());
                boolean isTarget = slots.contains(edge.getValue());
                if (isSource != isTarget) {
                    canvasUI.getGraph().disconnect(edge.getKey(), edge.getValue(), true);
                }
            }
        }
    }

    private void deleteNode() {
        if (!GraphEditorUISettings.getInstance().isAskOnDeleteNode() || JOptionPane.showConfirmDialog(canvasUI.getDesktopWorkbench().getWindow(),
                "Do you really want to remove the node?", "Delete node",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Set<JIPipeGraphNode> nodes = Collections.singleton(node);
            UUID compartment = nodes.stream().map(JIPipeGraphNode::getUUIDInParentGraph).findFirst().orElse(null);
            if (canvasUI.getHistoryJournal() != null) {
                canvasUI.getHistoryJournal().snapshotBeforeRemoveNodes(nodes, compartment);
            }
            canvasUI.getGraph().removeNodes(nodes, true);
        }
    }

    private void initializeRibbonNodeTask(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task nodeTask = ribbon.addTask("Node");
        if (node instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm algorithm = (JIPipeAlgorithm) node;
            JIPipeDesktopRibbon.Band workloadBand = nodeTask.addBand("Workload");
            List<JMenuItem> runMenuItems = new ArrayList<>();
            for (NodeUIContextAction entry : JIPipeDesktopGraphNodeUI.RUN_NODE_CONTEXT_MENU_ENTRIES) {
                if (entry == null)
                    runMenuItems.add(null);
                else {
                    JMenuItem item = new JMenuItem(entry.getName(), entry.getIcon());
                    item.setToolTipText(entry.getDescription());
                    item.setAccelerator(entry.getKeyboardShortcut());
                    item.addActionListener(e -> {
                        JIPipeDesktopGraphNodeUI nodeUI = canvasUI.getNodeUIs().get(node);
                        if (entry.matches(Collections.singleton(nodeUI))) {
                            entry.run(canvasUI, Collections.singleton(nodeUI));
                        } else {
                            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                                    "Could not run this operation",
                                    entry.getName(),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    runMenuItems.add(item);
                }
            }
            workloadBand.add(new JIPipeDesktopLargeButtonRibbonAction("Run node", "Runs the node", UIUtils.getIcon32FromResources("actions/play.png"),
                    runMenuItems.toArray(new JMenuItem[0])));
            if (algorithm.canPassThrough()) {
                workloadBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Pass-through",
                        "If enabled, the inputs of the node will be passed through to the outputs without any changes",
                        algorithm.isPassThrough() ? UIUtils.getIconFromResources("emblems/checkbox-checked.png") : UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"),
                        algorithm.isPassThrough(),
                        (button) -> node.setParameter("jipipe:algorithm:pass-through", button.isSelected())));
            } else {
                workloadBand.add(new JIPipeDesktopSmallButtonRibbonAction("Pass-through", "Not available for this node. If enabled, the inputs of the node will be passed through to the outputs without any changes.",
                        UIUtils.getIconFromResources("emblems/checkbox-disabled.png"), () -> {
                }));
            }
            workloadBand.add(new JIPipeDesktopSmallToggleButtonRibbonAction("Enabled",
                    "Determined whether the algorithm is enabled",
                    algorithm.isEnabled() ? UIUtils.getIconFromResources("emblems/checkbox-checked.png") : UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"),
                    algorithm.isEnabled(),
                    (button) -> node.setParameter("jipipe:algorithm:enabled", button.isSelected())));
        }
        if (!getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
            JIPipeDesktopRibbon.Band learnBand = nodeTask.addBand("Learn");
            learnBand.add(new JIPipeDesktopLargeButtonRibbonAction("Load example", "Loads an example parameter set for this node", UIUtils.getIcon32FromResources("actions/graduation-cap.png"), this::loadExample));
        }
        JIPipeDesktopRibbon.Band shareBand = nodeTask.addBand("Share");
        shareBand.add(new JIPipeDesktopLargeButtonRibbonAction("Create template", "Saves this node as template that can be re-used and shared with others", UIUtils.getIcon32FromResources("actions/star.png"), this::createNodeTemplate));
        shareBand.add(new JIPipeDesktopLargeButtonRibbonAction("Copy", "Copies the node into the clipboard. The generated text can be pasted into other people's JIPipe projects", UIUtils.getIcon32FromResources("actions/edit-copy.png"), this::copyNode));

        if (node instanceof JIPipeNodeGroup) {
            JIPipeDesktopRibbon.Band groupBand = nodeTask.addBand("Group");
            groupBand.add(new JIPipeDesktopLargeButtonRibbonAction("Edit contents", "Edits the contents of the group", UIUtils.getIcon32FromResources("actions/edit.png"), this::editNodeGroupContents));
        }
    }

    private void editNodeGroupContents() {
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            JIPipeDesktopNodeGroupUI.openGroupNodeGraph(getDesktopWorkbench(), (JIPipeNodeGroup) node, true);
        }
    }

    private void loadExample() {
        JIPipeNodeExamplePickerDialog pickerDialog = new JIPipeNodeExamplePickerDialog(getDesktopWorkbench().getWindow());
        pickerDialog.setTitle("Load example");
        List<JIPipeNodeExample> nodeExamples = getProject().getNodeExamples(node.getInfo().getId());
        pickerDialog.setAvailableItems(nodeExamples);
        JIPipeNodeExample selection = pickerDialog.showDialog();
        if (selection != null) {
            ((JIPipeAlgorithm) node).loadExample(selection);
            reload();
            parentPanel.getTabbedPane().selectSingletonTab("PARAMETERS");
        }
    }

    private void copyNode() {
        try {
            JIPipeGraph graph = canvasUI.getGraph()
                    .extract(Collections.singleton(node), true, false);
            String json = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(graph);
            StringSelection stringSelection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);
            canvasUI.getDesktopWorkbench().sendStatusBarText("Copied node");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void createNodeTemplate() {
        JIPipeNodeTemplate.create(parentPanel.getCanvas(), Collections.singleton(parentPanel.getNode()));
    }

    private void initializeDocumentation(JIPipeDesktopFormPanel formPanel) {
        JIPipeGraphNode node = parentPanel.getNode();
        JIPipeDesktopFormPanel.GroupHeaderPanel documentationGroup = formPanel.addGroupHeader(node.getName(), JIPipe.getNodes().getIconFor(node.getInfo()));
        documentationGroup.addColumn(UIUtils.createButton("Full documentation", UIUtils.getIconFromResources("actions/open-in-new-window.png"), this::openFullDocumentation));

        String description;
        if (StringUtils.isNullOrEmpty(node.getCustomDescription().getBody())) {
            description = node.getInfo().getDescription().getHtml();
        } else {
            description = node.getCustomDescription().getHtml();
        }
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane(description, false));

        JIPipeDesktopFormPanel ioTable = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
        for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
            JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(inputSlot.getAcceptedDataType());
            ioTable.addToForm(UIUtils.makeBorderlessReadonlyTextPane(dataInfo.getDescription() + ". " + StringUtils.nullToEmpty(inputSlot.getDescription()), false),
                    new JLabel("In: " + StringUtils.orElse(inputSlot.getInfo().getCustomName(), inputSlot.getName()), JIPipe.getDataTypes().getIconFor(inputSlot.getAcceptedDataType()), JLabel.LEFT));
        }
        for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
            JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(outputSlot.getAcceptedDataType());
            ioTable.addToForm(UIUtils.makeBorderlessReadonlyTextPane(dataInfo.getDescription() + ". " + StringUtils.nullToEmpty(outputSlot.getDescription()), false),
                    new JLabel("Out: " + StringUtils.orElse(outputSlot.getInfo().getCustomName(), outputSlot.getName()), JIPipe.getDataTypes().getIconFor(outputSlot.getAcceptedDataType()), JLabel.LEFT));
        }

        formPanel.addWideToForm(ioTable);
    }

    private void openFullDocumentation() {
        JIPipeDesktopAlgorithmCompendiumUI compendiumUI = new JIPipeDesktopAlgorithmCompendiumUI();
        compendiumUI.selectItem(parentPanel.getNode().getInfo());
        getDesktopWorkbench().getDocumentTabPane().addTab("Node documentation",
                UIUtils.getIconFromResources("actions/help.png"),
                compendiumUI,
                JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                true);
        getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (!isDisplayable()) {
            node.getParameterChangedEventEmitter().unsubscribe(this);
            return;
        }
        if ("jipipe:algorithm:enabled".equals(event.getKey()) || "jipipe:algorithm:pass-through".equals(event.getKey()) || "jipipe:node:ui-locked".equals(event.getKey())) {
            SwingUtilities.invokeLater(this::reload);
        }
    }

    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        if (!isDisplayable()) {
            getProject().getCache().getModifiedEventEmitter().unsubscribe(this);
            return;
        }
        SwingUtilities.invokeLater(this::reload);
    }
}
