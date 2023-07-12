/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties;

import com.google.common.collect.ImmutableSet;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.nodetemplate.NodeTemplateBox;
import org.hkijena.jipipe.extensions.nodetoolboxtool.NodeToolBox;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.bookmarks.BookmarkListPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorMinimap;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.history.HistoryJournalUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * UI when multiple algorithms are selected
 */
public class JIPipePipelineMultiAlgorithmSelectionPanelUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeGraph graph;
    private final JIPipeGraphCanvasUI canvas;
    private final Set<JIPipeGraphNode> nodes;

    /**
     * @param workbenchUI The workbench
     * @param canvas      The algorithm graph
     * @param nodes       The algorithm selection
     */
    public JIPipePipelineMultiAlgorithmSelectionPanelUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphCanvasUI canvas, Set<JIPipeGraphNode> nodes) {
        super(workbenchUI);
        this.graph = canvas.getGraph();
        this.canvas = canvas;
        this.nodes = nodes;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new BorderLayout());

        DocumentTabPane tabPane = new DocumentTabPane(false);
        tabPane.addTab("Selection", UIUtils.getIconFromResources("actions/edit-select-all.png"), actionPanel, DocumentTabPane.CloseMode.withoutCloseButton);

        tabPane.addTab("Available nodes", UIUtils.getIconFromResources("actions/graph-node.png"),
                new NodeToolBox(getWorkbench(), true), DocumentTabPane.CloseMode.withoutCloseButton);

        tabPane.addTab("Node templates", UIUtils.getIconFromResources("actions/favorite.png"),
                new NodeTemplateBox(getWorkbench(), true), DocumentTabPane.CloseMode.withoutCloseButton);

        tabPane.addTab("Bookmarks", UIUtils.getIconFromResources("actions/bookmarks.png"),
                new BookmarkListPanel(getWorkbench(), canvas.getGraph(), canvas.getGraphEditorUI()), DocumentTabPane.CloseMode.withoutCloseButton);

        tabPane.addTab("Journal",
                UIUtils.getIconFromResources("actions/edit-undo-history.png"),
                new HistoryJournalUI(canvas.getHistoryJournal()),
                DocumentTabPane.CloseMode.withoutCloseButton);

        splitPane.setBottomComponent(tabPane);
        splitPane.setTopComponent(new JIPipeGraphEditorMinimap(canvas.getGraphEditorUI()));

        initializeToolbar(actionPanel);
        initializeActionPanel(actionPanel);
    }

    private void initializeActionPanel(JPanel actionPanel) {
        FormPanel content = new FormPanel(FormPanel.WITH_SCROLLING);
        Set<JIPipeGraphNodeUI> nodeUIs = canvas.getNodeUIsFor(nodes);
        boolean canAddSeparator = false;
        for (NodeUIContextAction action : canvas.getContextActions()) {
            if (action == null) {
                if (canAddSeparator) {
                    content.addWideToForm(new JSeparator());
                    canAddSeparator = false;
                }
                continue;
            }
            if (action.isHidden())
                continue;
            if (!action.showInMultiSelectionPanel())
                continue;
            boolean matches = action.matches(nodeUIs);
            if (!matches && !action.disableOnNonMatch())
                continue;

            JButton item = new JButton("<html>" + action.getName() + "<br/><small>" + action.getDescription() + "</small></html>", action.getIcon());
            item.setHorizontalAlignment(SwingConstants.LEFT);
            item.setToolTipText(action.getDescription());
            if (matches) {
                item.addActionListener(e -> action.run(canvas, ImmutableSet.copyOf(nodeUIs)));
                content.addWideToForm(item);
                canAddSeparator = true;
            }
        }
        content.addVerticalGlue();
        actionPanel.add(content, BorderLayout.CENTER);
    }

    private void initializeToolbar(JPanel actionPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(nodes.size() + " nodes", UIUtils.getIconFromResources("actions/edit-select-all.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(nodes),
                canvas.getContextActions(),
                canvas);

        actionPanel.add(toolBar, BorderLayout.NORTH);
    }
}
