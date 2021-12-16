package org.hkijena.jipipe.ui.bookmarks;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI.RUN_NODE_CONTEXT_MENU_ENTRIES;

public class BookmarkListPanel extends JIPipeWorkbenchPanel {

    private final JIPipeGraph graph;
    private final JIPipeGraphEditorUI graphEditorUI;
    private final JList<JIPipeGraphNode> nodeJList = new JList<>();
    private final Timer reloadTimer;
    private JButton runButton;

    /**
     * @param workbench the workbench
     * @param graph the graph where bookmarks are tracked
     * @param graphEditorUI the canvas. can be null.
     */
    public BookmarkListPanel(JIPipeWorkbench workbench, JIPipeGraph graph, JIPipeGraphEditorUI graphEditorUI) {
        super(workbench);
        this.graph = graph;
        this.graphEditorUI = graphEditorUI;

        reloadTimer = new Timer(1000, e -> reloadList());
        reloadTimer.setRepeats(false);

        initialize();
        reloadList();
        graph.getEventBus().register(this);
        if(graph.getProject() != null) {
            graph.getProject().getCompartmentGraph().getEventBus().register(this);
        }
        registerNodeEvents();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton removeButton = new JButton("Remove selected", UIUtils.getIconFromResources("actions/bookmark-remove.png"));
        removeButton.addActionListener(e -> removeSelectedBookmarks());
        toolBar.add(removeButton);

        toolBar.add(Box.createHorizontalGlue());

        runButton = new JButton("Run", UIUtils.getIconFromResources("actions/run-play.png"));
        JPopupMenu runMenu = UIUtils.addPopupMenuToComponent(runButton);
        for (NodeUIContextAction entry : RUN_NODE_CONTEXT_MENU_ENTRIES) {
            if (entry == null)
                runMenu.addSeparator();
            else {
                JMenuItem item = new JMenuItem(entry.getName(), entry.getIcon());
                item.setToolTipText(entry.getDescription());
                item.setAccelerator(entry.getKeyboardShortcut());
                item.addActionListener(e -> {
                   runSelectedNode(entry);
                });
                runMenu.add(item);
            }
        }
        if(graph.getProject() != null) {
            toolBar.add(runButton);
        }

        JButton goToButton = new JButton("Go to bookmark", UIUtils.getIconFromResources("actions/go-jump.png"));
        goToButton.addActionListener(e -> goToBookmark(nodeJList.getSelectedValue()));
        toolBar.add(goToButton);

        add(toolBar, BorderLayout.NORTH);

        nodeJList.setCellRenderer(new BookmarkedNodeListCellRenderer());
        nodeJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    goToBookmark(nodeJList.getSelectedValue());
                }
            }
        });
        nodeJList.addListSelectionListener(e -> runButton.setVisible(nodeJList.getSelectedValue() instanceof JIPipeAlgorithm));
        add(new JScrollPane(nodeJList), BorderLayout.CENTER);
    }

    private void runSelectedNode(NodeUIContextAction entry) {
        JIPipeGraphNode node = nodeJList.getSelectedValue();
        if(node instanceof JIPipeAlgorithm)  {
            goToBookmark(node);
            JIPipeProjectCompartment compartment = node.getProjectCompartment();
            DocumentTabPane.DocumentTab tab = ((JIPipeProjectWorkbench) getWorkbench()).getOrOpenPipelineEditorTab(compartment, true);
            JIPipePipelineGraphEditorUI editorUI = (JIPipePipelineGraphEditorUI) tab.getContent();
            JIPipeNodeUI ui = editorUI.getCanvasUI().getNodeUIs().getOrDefault(node, null);
            if(ui != null) {
                entry.run(editorUI.getCanvasUI(), Collections.singleton(ui));
            }
            else {
                getWorkbench().sendStatusBarText("Unable to run selected bookmark!");
            }
        }
        else {
            getWorkbench().sendStatusBarText("Unable to run selected bookmark!");
        }
    }

    private void removeSelectedBookmarks() {
        List<JIPipeGraphNode> selection = nodeJList.getSelectedValuesList();
        if(!selection.isEmpty()) {
            if(JOptionPane.showConfirmDialog(this, "Do you really want to remove the selected bookmarks?", "Remove bookmarks", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
            for (JIPipeGraphNode node : selection) {
                node.setBookmarked(false);
                node.triggerParameterChange("jipipe:node:bookmarked");
            }
            reloadTimer.stop();
            reloadList();
        }
    }

    private void goToBookmark(JIPipeGraphNode node) {
        if(node.getGraph() == null)
            return;
        if(getWorkbench() instanceof JIPipeProjectWorkbench && graph.getProject() != null) {
            if(node instanceof JIPipeProjectCompartment) {
                ((JIPipeProjectWorkbench) getWorkbench()).getOrOpenPipelineEditorTab((JIPipeProjectCompartment) node, true);
            }
            else {
                JIPipeProjectCompartment compartment = node.getProjectCompartment();
                DocumentTabPane.DocumentTab tab = ((JIPipeProjectWorkbench) getWorkbench()).getOrOpenPipelineEditorTab(compartment, true);
                SwingUtilities.invokeLater(() -> {
                    JIPipePipelineGraphEditorUI editorUI = (JIPipePipelineGraphEditorUI) tab.getContent();
                    JIPipeNodeUI ui = editorUI.getCanvasUI().getNodeUIs().getOrDefault(node, null);
                    if(ui != null) {
                        editorUI.selectOnly(ui);
                    }
                    else {
                        getWorkbench().sendStatusBarText("Unable to navigate to bookmark");
                    }
                });
            }
        }
        else if(graphEditorUI != null) {
            JIPipeNodeUI ui = graphEditorUI.getCanvasUI().getNodeUIs().getOrDefault(node, null);
            if(ui != null) {
                graphEditorUI.selectOnly(ui);
            }
            else {
                getWorkbench().sendStatusBarText("Unable to navigate to bookmark");
            }
        }
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public JIPipeGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    private void reloadList() {
        DefaultListModel<JIPipeGraphNode> model = new DefaultListModel<>();
        JIPipeProject project = graph.getProject();
        if(project != null) {
            project.getCompartments().values().stream().filter(JIPipeGraphNode::isBookmarked).sorted(Comparator.comparing(JIPipeGraphNode::getName)).forEach(model::addElement);
        }
        graph.getGraphNodes().stream().filter(JIPipeGraphNode::isBookmarked).sorted(Comparator.comparing(JIPipeGraphNode::getName)).forEach(model::addElement);
        nodeJList.setModel(model);
        runButton.setVisible(nodeJList.getSelectedValue() instanceof JIPipeAlgorithm);
    }

    private void registerNodeEvents() {
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            node.getEventBus().register(this);
        }
        if(graph.getProject() != null) {
            for (JIPipeGraphNode node : graph.getProject().getCompartmentGraph().getGraphNodes()) {
                node.getEventBus().register(this);
            }
        }
    }

    @Subscribe
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        registerNodeEvents();
        reloadTimer.restart();
    }

    @Subscribe
    public void onNodeParametersChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if("jipipe:node:bookmarked".equals(event.getKey())) {
            reloadTimer.restart();
        }
        else if("jipipe:node:description".equals(event.getKey())) {
            reloadTimer.restart();
        }
    }
}
