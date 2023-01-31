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
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

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
     * @param nodes  The algorithm selection
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
        initializeToolbar();
        initializeActionPanel();
    }

    private void initializeActionPanel() {
        FormPanel content = new FormPanel(FormPanel.WITH_SCROLLING);
        Set<JIPipeNodeUI> nodeUIs = canvas.getNodeUIsFor(nodes);
        boolean canAddSeparator = false;
        for (NodeUIContextAction action : canvas.getContextActions()) {
            if (action == null) {
                if(canAddSeparator) {
                    content.addWideToForm(new JSeparator());
                    canAddSeparator = false;
                }
                continue;
            }
            if (action.isHidden())
                continue;
            if(!action.showInMultiSelectionPanel())
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
        add(content, BorderLayout.CENTER);
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(nodes.size() + " nodes", UIUtils.getIconFromResources("actions/edit-select-all.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(nodes),
                canvas.getContextActions(),
                canvas);

        add(toolBar, BorderLayout.NORTH);
    }
}
