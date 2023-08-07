package org.hkijena.jipipe.ui.grapheditor.nodefinder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.ExistingPipelineNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.UUID;

public class JIPipeNodeFinderDialogUI extends JDialog {
    private static String LAST_SEARCH = "";
    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeDataSlot querySlot;
    private final JIPipeGraph queryGraph;
    private final UUID queryCompartment;
    private final JToggleButton findExistingNodesToggle = new JToggleButton(UIUtils.getIconFromResources("actions/find.png"));
    private final JToggleButton createNodesToggle = new JToggleButton(UIUtils.getIconFromResources("actions/add.png"));
    private SearchTextField searchField;
    private final JList<JIPipeNodeDatabaseEntry> nodeList = new JList<>();
    private final JIPipeRunnerQueue queue = new JIPipeRunnerQueue("Node finder");

    public JIPipeNodeFinderDialogUI(JIPipeGraphCanvasUI canvasUI, JIPipeDataSlot querySlot) {
        this.canvasUI = canvasUI;
        this.queryCompartment = canvasUI.getCompartment();
        this.querySlot = querySlot;
        this.queryGraph = canvasUI.getGraph();
        initialize();
        reloadList();
    }

    private void initialize() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        setContentPane(contentPanel);

        initializeToolBar();
        initializeList();

        setIconImage(UIUtils.getJIPipeIcon128());
        if(querySlot != null) {
            setTitle("Find matching " + (querySlot.isInput() ? "source" : "target"));
        }
        else {
            setTitle("Find node");
        }
        pack();
        setSize(800, 600);
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(canvasUI));
        setModal(true);
        UIUtils.addEscapeListener(this);
    }

    private void initializeList() {
        JIPipeNodeDatabase nodeDatabase;
        if(canvasUI.getWorkbench() instanceof JIPipeProjectWorkbench) {
            nodeDatabase = ((JIPipeProjectWorkbench) canvasUI.getWorkbench()).getNodeDatabase();
        }
        else {
            nodeDatabase = JIPipeNodeDatabase.getInstance();
        }

        nodeList.setOpaque(false);
        nodeList.setCellRenderer(new NodeFinderDatasetListCellRenderer(nodeDatabase));
        getContentPane().add(new JScrollPane(nodeList), BorderLayout.CENTER);

        nodeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
                    onItemClicked(e);
                }
            }
        });
    }

    private void onItemClicked(MouseEvent e) {
        int index = nodeList.locationToIndex(e.getPoint());
        if(index >= 0) {
            JIPipeNodeDatabaseEntry entry = nodeList.getModel().getElementAt(index);
            nodeList.setSelectedIndex(index);
            openEntryMenu(entry, e.getPoint());
        }
    }

    private void openEntryMenu(JIPipeNodeDatabaseEntry entry, Point location) {
        JPopupMenu menu = new JPopupMenu();
        if(!entry.exists()) {
            menu.add(UIUtils.createMenuItem("Add new to graph", "Adds the selected node", UIUtils.getIconFromResources("actions/add.png"), () -> addEntryToGraph(entry)));
        }
        if(querySlot != null) {
            if(querySlot.isInput()) {
                if((!entry.getInputSlots().isEmpty() || entry.canAddOutputSlots()) && menu.getComponentCount() > 0) {
                    menu.addSeparator();
                }
                for (Map.Entry<String, JIPipeDataSlotInfo> infoEntry : entry.getOutputSlots().entrySet()) {
                    menu.add(UIUtils.createMenuItem("Connect to " + infoEntry.getKey(),
                            "Connect to the specified slot. Add the node if required.",
                            JIPipe.getDataTypes().getIconFor(infoEntry.getValue().getDataClass()),
                            () -> addAndConnectEntry(entry, infoEntry.getValue())));
                }
                if(entry.canAddOutputSlots()) {
                    menu.addSeparator();
                    menu.add(UIUtils.createMenuItem("Connect to new slot",
                            "Connect to a new slot. Add the node if required.",
                            UIUtils.getIconFromResources("actions/add.png"),
                            () -> addAndConnectEntry(entry, null)));
                }
            }
            else {
                if((!entry.getInputSlots().isEmpty() || entry.canAddInputSlots()) && menu.getComponentCount() > 0) {
                    menu.addSeparator();
                }
                for (Map.Entry<String, JIPipeDataSlotInfo> infoEntry : entry.getInputSlots().entrySet()) {
                    menu.add(UIUtils.createMenuItem("Connect to " + infoEntry.getKey(),
                            "Connect to the specified slot. Add the node if required.",
                            JIPipe.getDataTypes().getIconFor(infoEntry.getValue().getDataClass()),
                            () -> addAndConnectEntry(entry, infoEntry.getValue())));
                }
                if(entry.canAddInputSlots()) {
                    menu.addSeparator();
                    menu.add(UIUtils.createMenuItem("Connect to new slot",
                            "Connect to a new slot. Add the node if required.",
                            UIUtils.getIconFromResources("actions/add.png"),
                            () -> addAndConnectEntry(entry, null)));
                }
            }

        }
        menu.show(nodeList, location.x, location.y);
    }

    private void addAndConnectEntry(JIPipeNodeDatabaseEntry entry, JIPipeDataSlotInfo slotInfo) {
        JIPipeGraphNodeUI nodeUI = entry.addToGraph(canvasUI);
        JIPipeGraphNode node = nodeUI.getNode();
        if(querySlot != null) {
            if(querySlot.isInput()) {
                if(slotInfo != null) {
                    JIPipeDataSlot targetSlot = node.getOutputSlot(slotInfo.getName());
                    if(targetSlot != null) {
                        canvasUI.getGraph().connect(targetSlot, querySlot);
                    }
                }
                else {
                    AddAlgorithmSlotPanel panel = AddAlgorithmSlotPanel.showDialog(SwingUtilities.getWindowAncestor(canvasUI),
                            canvasUI.getHistoryJournal(),
                            node,
                            JIPipeSlotType.Output);
                    if(!panel.getAddedSlots().isEmpty()) {
                        canvasUI.getGraph().connect(panel.getAddedSlots().get(0), querySlot);
                    }
                }
            }
            else {
                if(slotInfo != null) {
                    JIPipeDataSlot targetSlot = node.getInputSlot(slotInfo.getName());
                    if(targetSlot != null) {
                        canvasUI.getGraph().connect(querySlot, targetSlot);
                    }
                }
                else {
                    AddAlgorithmSlotPanel panel = AddAlgorithmSlotPanel.showDialog(SwingUtilities.getWindowAncestor(canvasUI),
                            canvasUI.getHistoryJournal(),
                            node,
                            JIPipeSlotType.Input);
                    if(!panel.getAddedSlots().isEmpty()) {
                        canvasUI.getGraph().connect(querySlot, panel.getAddedSlots().get(0));
                    }
                }
            }
        }
        LAST_SEARCH = searchField.getText();
        setVisible(false);
    }

    private void addEntryToGraph(JIPipeNodeDatabaseEntry entry) {
        entry.addToGraph(canvasUI);
        LAST_SEARCH = searchField.getText();
        setVisible(false);
    }

    private void initializeToolBar() {

        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));

        // Info toolbar
        if(querySlot != null) {
            JIPipeGraphNode queryNode = querySlot.getNode();
            JToolBar infoToolbar = new JToolBar();
            infoToolbar.setFloatable(false);
            infoToolbar.add(new JLabel("Selected slot"));
            infoToolbar.add(Box.createRigidArea(new Dimension(8, 32)));
            JLabel algorithmNameLabel = new JLabel(queryNode.getName(), JIPipe.getNodes().getIconFor(queryNode.getInfo()), JLabel.LEFT);
            algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(queryNode.getInfo()));
            infoToolbar.add(algorithmNameLabel);
            JLabel arrowLabel = new JLabel(UIUtils.getIconFromResources("actions/draw-triangle2.png"));
            infoToolbar.add(arrowLabel);
            JLabel slotNameLabel = new JLabel(querySlot.getName(), JIPipe.getDataTypes().getIconFor(querySlot.getAcceptedDataType()), JLabel.LEFT);
            slotNameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(querySlot));
            infoToolbar.add(slotNameLabel);
            toolbarPanel.add(infoToolbar);
        }

        // Main toolbar
        JToolBar mainToolBar = new JToolBar();
        mainToolBar.setFloatable(false);
        mainToolBar.add(Box.createHorizontalStrut(8));

        searchField = new SearchTextField();
        searchField.setText(LAST_SEARCH);
        searchField.addActionListener(e -> reloadList());
        searchField.getTextField().selectAll();
        searchField.getTextField().addActionListener(e -> openFirstEntryMenu());
        mainToolBar.add(searchField);

        if(querySlot != null) {
            findExistingNodesToggle.setText("Find existing");
            findExistingNodesToggle.setSelected(GraphEditorUISettings.getInstance().getAlgorithmFinderSettings().isSearchFindExistingNodes());
            createNodesToggle.setText("Create new");
            createNodesToggle.setSelected(GraphEditorUISettings.getInstance().getAlgorithmFinderSettings().isSearchFindNewNodes());
            findExistingNodesToggle.addActionListener(e -> {
                GraphEditorUISettings.getInstance().getAlgorithmFinderSettings().setSearchFindExistingNodes(findExistingNodesToggle.isSelected());
                JIPipe.getSettings().save();
                reloadList();
            });
            createNodesToggle.addActionListener(e -> {
                GraphEditorUISettings.getInstance().getAlgorithmFinderSettings().setSearchFindNewNodes(createNodesToggle.isSelected());
                JIPipe.getSettings().save();
                reloadList();
            });
            mainToolBar.add(findExistingNodesToggle);
            mainToolBar.add(createNodesToggle);
        }
        mainToolBar.add(Box.createHorizontalStrut(8));

        toolbarPanel.add(mainToolBar);

        getContentPane().add(toolbarPanel, BorderLayout.NORTH);
    }

    private void openFirstEntryMenu() {
        if(nodeList.getModel().getSize() > 0) {
            nodeList.setSelectedIndex(0);
            Point point = nodeList.indexToLocation(0);
            point.x += nodeList.getWidth() - 64;
            point.y += 100;
            openEntryMenu(nodeList.getModel().getElementAt(0), point);
        }
    }

    private void reloadList() {
        queue.cancelAll();
        queue.enqueue(new ReloadListRun(this));
    }

    public static class ReloadListRun extends AbstractJIPipeRunnable {

        private final JIPipeNodeFinderDialogUI dialogUI;

        public ReloadListRun(JIPipeNodeFinderDialogUI dialogUI) {
            this.dialogUI = dialogUI;
        }

        @Override
        public String getTaskLabel() {
            return "Reload list";
        }

        @Override
        public void run() {
            JIPipeGraphCanvasUI canvasUI = dialogUI.canvasUI;
            JIPipeDataSlot querySlot = dialogUI.querySlot;
            DefaultListModel<JIPipeNodeDatabaseEntry> model = new DefaultListModel<>();
            JIPipeNodeDatabase nodeDatabase;
            if(canvasUI.getWorkbench() instanceof JIPipeProjectWorkbench) {
                nodeDatabase = ((JIPipeProjectWorkbench) canvasUI.getWorkbench()).getNodeDatabase();
            }
            else {
                nodeDatabase = JIPipeNodeDatabase.getInstance();
            }
            JIPipeNodeDatabaseRole role;
            if(canvasUI.getWorkbench() instanceof JIPipeProjectWorkbench) {
                if(canvasUI.getGraph() == ((JIPipeProjectWorkbench) canvasUI.getWorkbench()).getProject().getCompartmentGraph()) {
                    role = JIPipeNodeDatabaseRole.CompartmentNode;
                }
                else {
                    role = JIPipeNodeDatabaseRole.PipelineNode;
                }
            }
            else {
                role = JIPipeNodeDatabaseRole.PipelineNode;
            }
            boolean allowExisting, allowNew;
            if(querySlot != null) {
                allowExisting = dialogUI.findExistingNodesToggle.isSelected();
                allowNew = dialogUI.createNodesToggle.isSelected();
            }
            else {
                allowExisting = false;
                allowNew = true;
            }
            if(querySlot != null) {
                for (JIPipeNodeDatabaseEntry entry : nodeDatabase.query(dialogUI.searchField.getText(),
                        role,
                        allowExisting,
                        allowNew,
                        querySlot.getSlotType(),
                        querySlot.getInfo().getDataClass())) {
                    if(entry instanceof ExistingPipelineNodeDatabaseEntry && ((ExistingPipelineNodeDatabaseEntry) entry).getGraphNode() == querySlot.getNode()) {
                        continue;
                    }
                    model.addElement(entry);
                }
            }
            else {
                for (JIPipeNodeDatabaseEntry entry : nodeDatabase.query(dialogUI.searchField.getText(), role, allowExisting, allowNew)) {
                    model.addElement(entry);
                }
            }

            dialogUI.nodeList.setModel(model);
        }
    }
}
