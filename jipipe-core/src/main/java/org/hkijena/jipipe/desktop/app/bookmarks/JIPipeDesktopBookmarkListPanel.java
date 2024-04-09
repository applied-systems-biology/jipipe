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

package org.hkijena.jipipe.desktop.app.bookmarks;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI.RUN_NODE_CONTEXT_MENU_ENTRIES;

public class JIPipeDesktopBookmarkListPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeGraph.GraphChangedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeGraph graph;
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final JList<JIPipeGraphNode> nodeJList = new JList<>();
    private final Timer reloadTimer;
    private final Set<JIPipeGraphNode> selectedNodes;
    private JButton runButton;

    /**
     * @param workbench     the workbench
     * @param graph         the graph where bookmarks are tracked
     * @param graphEditorUI the canvas. can be null.
     */
    public JIPipeDesktopBookmarkListPanel(JIPipeDesktopWorkbench workbench, JIPipeGraph graph, JIPipeDesktopGraphEditorUI graphEditorUI, Set<JIPipeGraphNode> selectedNodes) {
        super(workbench);
        this.graph = graph;
        this.graphEditorUI = graphEditorUI;
        this.selectedNodes = selectedNodes;

        reloadTimer = new Timer(1000, e -> reloadList());
        reloadTimer.setRepeats(false);

        initialize();
        reloadList();
        graph.getGraphChangedEventEmitter().subscribeWeak(this);
        if (graph.getProject() != null) {
            graph.getProject().getCompartmentGraph().getGraphChangedEventEmitter().subscribeWeak(this);
        }
        registerNodeEvents();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(UIUtils.createButton("Reload", UIUtils.getIconFromResources("actions/reload.png"), this::reloadList));

        toolBar.add(Box.createHorizontalGlue());

        if (selectedNodes != null && !selectedNodes.isEmpty()) {
            JButton removeButton = new JButton("Add", UIUtils.getIconFromResources("actions/bookmark.png"));
            removeButton.addActionListener(e -> addSelectionAsBookmark());
            toolBar.add(removeButton);
        }

        JButton removeButton = new JButton("Remove", UIUtils.getIconFromResources("actions/bookmark-remove.png"));
        removeButton.addActionListener(e -> removeSelectedBookmarks());
        toolBar.add(removeButton);

        runButton = new JButton("Run", UIUtils.getIconFromResources("actions/run-play.png"));
        JPopupMenu runMenu = UIUtils.addPopupMenuToButton(runButton);
        for (NodeUIContextAction entry : RUN_NODE_CONTEXT_MENU_ENTRIES) {
            if (entry == null)
                runMenu.addSeparator();
            else {
                JMenuItem item = new JMenuItem(entry.getName(), entry.getIcon());
                item.setToolTipText(entry.getDescription());
                item.addActionListener(e -> {
                    runSelectedNode(entry);
                });
                runMenu.add(item);
            }
        }
        if (graph.getProject() != null) {
            toolBar.add(runButton);
        }

        JButton goToButton = new JButton("Go to bookmark", UIUtils.getIconFromResources("actions/go-jump.png"));
        goToButton.addActionListener(e -> goToBookmark(nodeJList.getSelectedValue()));
        toolBar.add(goToButton);

        add(toolBar, BorderLayout.NORTH);

        nodeJList.setCellRenderer(new JIPipeDesktopBookmarkedNodeListCellRenderer());
        nodeJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    goToBookmark(nodeJList.getSelectedValue());
                }
            }
        });
        nodeJList.addListSelectionListener(e -> runButton.setVisible(nodeJList.getSelectedValue() instanceof JIPipeAlgorithm));
        add(new JScrollPane(nodeJList), BorderLayout.CENTER);
    }

    private void addSelectionAsBookmark() {
        for (JIPipeGraphNode node : selectedNodes) {
            node.setParameter("jipipe:node:bookmarked", true);
        }
        reloadList();
    }

    private void runSelectedNode(NodeUIContextAction entry) {
        JIPipeGraphNode node = nodeJList.getSelectedValue();
        if (node instanceof JIPipeAlgorithm) {
            goToBookmark(node);
            JIPipeProjectCompartment compartment = node.getProjectCompartment();
            JIPipeDesktopTabPane.DocumentTab tab = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getOrOpenPipelineEditorTab(compartment, true);
            JIPipePipelineGraphEditorUI editorUI = (JIPipePipelineGraphEditorUI) tab.getContent();
            JIPipeDesktopGraphNodeUI ui = editorUI.getCanvasUI().getNodeUIs().getOrDefault(node, null);
            if (ui != null) {
                entry.run(editorUI.getCanvasUI(), Collections.singleton(ui));
            } else {
                getDesktopWorkbench().sendStatusBarText("Unable to run selected bookmark!");
            }
        } else {
            getDesktopWorkbench().sendStatusBarText("Unable to run selected bookmark!");
        }
    }

    private void removeSelectedBookmarks() {
        List<JIPipeGraphNode> selection = nodeJList.getSelectedValuesList();
        if (!selection.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this, "Do you really want to remove the selected bookmarks?", "Remove bookmarks", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
            for (JIPipeGraphNode node : selection) {
                node.setBookmarked(false);
                node.emitParameterChangedEvent("jipipe:node:bookmarked");
            }
            reloadTimer.stop();
            reloadList();
        }
    }

    private void goToBookmark(JIPipeGraphNode node) {
        if (node.getParentGraph() == null)
            return;
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench && graph.getProject() != null) {
            if (node instanceof JIPipeProjectCompartment) {
                ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getOrOpenPipelineEditorTab((JIPipeProjectCompartment) node, true);
            } else {
                JIPipeProjectCompartment compartment = node.getProjectCompartment();
                JIPipeDesktopTabPane.DocumentTab tab = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getOrOpenPipelineEditorTab(compartment, true);
                SwingUtilities.invokeLater(() -> {
                    JIPipePipelineGraphEditorUI editorUI = (JIPipePipelineGraphEditorUI) tab.getContent();
                    JIPipeDesktopGraphNodeUI ui = editorUI.getCanvasUI().getNodeUIs().getOrDefault(node, null);
                    if (ui != null) {
                        editorUI.selectOnly(ui);
                    } else {
                        getDesktopWorkbench().sendStatusBarText("Unable to navigate to bookmark");
                    }
                });
            }
        } else if (graphEditorUI != null) {
            JIPipeDesktopGraphNodeUI ui = graphEditorUI.getCanvasUI().getNodeUIs().getOrDefault(node, null);
            if (ui != null) {
                graphEditorUI.selectOnly(ui);
            } else {
                getDesktopWorkbench().sendStatusBarText("Unable to navigate to bookmark");
            }
        }
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    private void reloadList() {
        DefaultListModel<JIPipeGraphNode> model = new DefaultListModel<>();
        JIPipeProject project = graph.getProject();
        if (project != null) {
            project.getCompartments().values().stream().filter(JIPipeGraphNode::isBookmarked).sorted(Comparator.comparing(JIPipeGraphNode::getName)).forEach(model::addElement);
        }
        graph.getGraphNodes().stream().filter(JIPipeGraphNode::isBookmarked).sorted(Comparator.comparing(JIPipeGraphNode::getName)).forEach(model::addElement);
        nodeJList.setModel(model);
        runButton.setVisible(nodeJList.getSelectedValue() instanceof JIPipeAlgorithm);
    }

    private void registerNodeEvents() {
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            node.getParameterChangedEventEmitter().subscribeWeak(this);
        }
        if (graph.getProject() != null) {
            for (JIPipeGraphNode node : graph.getProject().getCompartmentGraph().getGraphNodes()) {
                node.getParameterChangedEventEmitter().subscribeWeak(this);
            }
        }
    }

    @Override
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        registerNodeEvents();
        reloadTimer.restart();
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("jipipe:node:bookmarked".equals(event.getKey())) {
            reloadTimer.restart();
        } else if ("jipipe:node:description".equals(event.getKey())) {
            reloadTimer.restart();
        }
    }
}
