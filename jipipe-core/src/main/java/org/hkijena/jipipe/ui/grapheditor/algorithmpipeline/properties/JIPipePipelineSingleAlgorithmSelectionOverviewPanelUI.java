package org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.nodeexamples.JIPipeNodeExamplePickerDialog;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ribbon.*;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.documentation.JIPipeAlgorithmCompendiumUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorMinimap;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.grouping.JIPipeNodeGroupUI;
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

public class JIPipePipelineSingleAlgorithmSelectionOverviewPanelUI extends JIPipeProjectWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener, JIPipeCache.ModifiedEventListener {

    private final JIPipePipelineSingleAlgorithmSelectionPanelUI parentPanel;
    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeGraphNode node;
    private final FormPanel formPanel = new FormPanel(FormPanel.WITH_SCROLLING);
    private final Ribbon ribbon = new Ribbon(2);

    public JIPipePipelineSingleAlgorithmSelectionOverviewPanelUI(JIPipePipelineSingleAlgorithmSelectionPanelUI parentPanel) {
        super(parentPanel.getProjectWorkbench());
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

        splitPane.setTopComponent(new JIPipeGraphEditorMinimap(parentPanel.getGraphEditorUI()));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        splitPane.setBottomComponent(bottomPanel);

        bottomPanel.add(formPanel, BorderLayout.CENTER);
        bottomPanel.add(ribbon, BorderLayout.NORTH);
    }

    private void reload() {
        ribbon.clear();
        initializeRibbon(ribbon);
        ribbon.rebuildRibbon();

        formPanel.clear();
        initializeDocumentation(formPanel);
        if (node instanceof NodeGroup) {
            initializeNodeGroup(formPanel);
        }
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

    private void initializeCache(FormPanel formPanel, Map<String, JIPipeDataTable> query) {
        FormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Results available", UIUtils.getIconFromResources("actions/database.png"));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("Previously generated results are stored in the memory cache. Click the 'Show results' button to review the results.", false));
        groupHeader.addColumn(UIUtils.createButton("Show results", UIUtils.getIconFromResources("actions/open-in-new-window.png"), this::openCacheBrowser));

        FormPanel ioTable = new FormPanel(FormPanel.NONE);
        for (JIPipeOutputDataSlot outputSlot : node.getOutputSlots()) {
            JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(outputSlot.getAcceptedDataType());
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

    private void initializeNodeGroup(FormPanel formPanel) {
        FormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Group contents", UIUtils.getIconFromResources("actions/help-info.png"));
        groupHeader.addColumn(UIUtils.createButton("Edit", UIUtils.getIconFromResources("actions/edit.png"), this::editNodeGroupContents));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("This node executes the content of a sub-pipeline. You can modify it by clicking the 'Edit' button.", false));

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
                node.isUiLocked(),
                (button) -> {
                    node.setParameter("jipipe:node:ui-locked", button.isSelected());
                }));
        Ribbon.Band deleteBand = editTask.addBand("Delete");
        deleteBand.add(new LargeButtonAction("Isolate node", "Removes all connections of this node", UIUtils.getIcon32FromResources("actions/network-disconnect.png"), this::isolateNode));
        if (!node.isUiLocked()) {
            deleteBand.add(new LargeButtonAction("Delete node", "Deletes the node", UIUtils.getIcon32FromResources("actions/trash.png"), this::deleteNode));
        }
//        Ribbon.Band miscBand = editTask.addBand("Misc");
    }

    private void isolateNode() {
        if (JOptionPane.showConfirmDialog(canvasUI.getWorkbench().getWindow(),
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
        if (!GraphEditorUISettings.getInstance().isAskOnDeleteNode() || JOptionPane.showConfirmDialog(canvasUI.getWorkbench().getWindow(),
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

    private void initializeRibbonNodeTask(Ribbon ribbon) {
        Ribbon.Task nodeTask = ribbon.addTask("Node");
        if (node instanceof JIPipeAlgorithm) {
            JIPipeAlgorithm algorithm = (JIPipeAlgorithm) node;
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
                        JIPipeGraphNodeUI nodeUI = canvasUI.getNodeUIs().get(node);
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
            workloadBand.add(new LargeButtonAction("Run node", "Runs the node", UIUtils.getIcon32FromResources("actions/play.png"),
                    runMenuItems.toArray(new JMenuItem[0])));
            if (algorithm.canPassThrough()) {
                workloadBand.add(new SmallToggleButtonAction("Pass-through",
                        "If enabled, the inputs of the node will be passed through to the outputs without any changes",
                        algorithm.isPassThrough() ? UIUtils.getIconFromResources("emblems/checkbox-checked.png") : UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"),
                        algorithm.isPassThrough(),
                        (button) -> node.setParameter("jipipe:algorithm:pass-through", button.isSelected())));
            } else {
                workloadBand.add(new SmallButtonAction("Pass-through", "Not available for this node. If enabled, the inputs of the node will be passed through to the outputs without any changes.",
                        UIUtils.getIconFromResources("emblems/checkbox-disabled.png"), () -> {
                }));
            }
            workloadBand.add(new SmallToggleButtonAction("Enabled",
                    "Determined whether the algorithm is enabled",
                    algorithm.isEnabled() ? UIUtils.getIconFromResources("emblems/checkbox-checked.png") : UIUtils.getIconFromResources("emblems/checkbox-unchecked.png"),
                    algorithm.isEnabled(),
                    (button) -> node.setParameter("jipipe:algorithm:enabled", button.isSelected())));
        }
        if (!getProject().getNodeExamples(node.getInfo().getId()).isEmpty()) {
            Ribbon.Band learnBand = nodeTask.addBand("Learn");
            learnBand.add(new LargeButtonAction("Load example", "Loads an example parameter set for this node", UIUtils.getIcon32FromResources("actions/graduation-cap.png"), this::loadExample));
        }
        Ribbon.Band shareBand = nodeTask.addBand("Share");
        shareBand.add(new LargeButtonAction("Create template", "Saves this node as template that can be re-used and shared with others", UIUtils.getIcon32FromResources("actions/star.png"), this::createNodeTemplate));
        shareBand.add(new LargeButtonAction("Copy", "Copies the node into the clipboard. The generated text can be pasted into other people's JIPipe projects", UIUtils.getIcon32FromResources("actions/edit-copy.png"), this::copyNode));

        if (node instanceof NodeGroup) {
            Ribbon.Band groupBand = nodeTask.addBand("Group");
            groupBand.add(new LargeButtonAction("Edit contents", "Edits the contents of the group", UIUtils.getIcon32FromResources("actions/edit.png"), this::editNodeGroupContents));
        }
    }

    private void editNodeGroupContents() {
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            JIPipeNodeGroupUI.openGroupNodeGraph(getWorkbench(), (NodeGroup) node, true);
        }
    }

    private void loadExample() {
        JIPipeNodeExamplePickerDialog pickerDialog = new JIPipeNodeExamplePickerDialog(getWorkbench().getWindow());
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
            canvasUI.getWorkbench().sendStatusBarText("Copied node");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void createNodeTemplate() {
        JIPipeNodeTemplate.create(parentPanel.getCanvas(), Collections.singleton(parentPanel.getNode()));
    }

    private void initializeDocumentation(FormPanel formPanel) {
        JIPipeGraphNode node = parentPanel.getNode();
        FormPanel.GroupHeaderPanel documentationGroup = formPanel.addGroupHeader(node.getName(), JIPipe.getNodes().getIconFor(node.getInfo()));
        documentationGroup.addColumn(UIUtils.createButton("Full documentation", UIUtils.getIconFromResources("actions/open-in-new-window.png"), this::openFullDocumentation));

        String description;
        if (StringUtils.isNullOrEmpty(node.getCustomDescription().getBody())) {
            description = node.getInfo().getDescription().getHtml();
        } else {
            description = node.getCustomDescription().getHtml();
        }
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane(description, false));

        FormPanel ioTable = new FormPanel(FormPanel.NONE);
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
        JIPipeAlgorithmCompendiumUI compendiumUI = new JIPipeAlgorithmCompendiumUI();
        compendiumUI.selectItem(parentPanel.getNode().getInfo());
        getWorkbench().getDocumentTabPane().addTab("Node documentation",
                UIUtils.getIconFromResources("actions/help.png"),
                compendiumUI,
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (!isDisplayable()) {
            node.getParameterChangedEventEmitter().unsubscribe(this);
            return;
        }
        if ("jipipe:algorithm:enabled".equals(event.getKey()) || "jipipe:algorithm:pass-through".equals(event.getKey()) || "jipipe:node:ui-locked".equals(event.getKey())) {
            reload();
        }
    }

    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        if (!isDisplayable()) {
            getProject().getCache().getModifiedEventEmitter().unsubscribe(this);
            return;
        }
        reload();
    }
}
