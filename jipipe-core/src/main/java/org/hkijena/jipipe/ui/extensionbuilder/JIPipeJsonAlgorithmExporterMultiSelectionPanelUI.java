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

package org.hkijena.jipipe.ui.extensionbuilder;

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panel shown when multiple algorithms are selected
 */
public class JIPipeJsonAlgorithmExporterMultiSelectionPanelUI extends JIPipeWorkbenchPanel {
    private JIPipeGraph graph;
    private JIPipeGraphCanvasUI canvas;
    private Set<JIPipeGraphNode> algorithms;

    /**
     * @param workbenchUI Workbench UI
     * @param canvas      The graph
     * @param algorithms  Selected algorithms
     */
    public JIPipeJsonAlgorithmExporterMultiSelectionPanelUI(JIPipeWorkbench workbenchUI, JIPipeGraphCanvasUI canvas, Set<JIPipeGraphNode> algorithms) {
        super(workbenchUI);
        this.graph = canvas.getGraph();
        this.canvas = canvas;
        this.algorithms = algorithms;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader content = new MarkdownReader(false);
        add(content, BorderLayout.CENTER);

        StringBuilder markdownContent = new StringBuilder();
        for (JIPipeGraphNode algorithm : algorithms.stream().sorted(Comparator.comparing(JIPipeGraphNode::getName)).collect(Collectors.toList())) {
            markdownContent.append(TooltipUtils.getAlgorithmTooltip(algorithm.getInfo())
                    .replace("<html>", "<div style=\"border: 1px solid gray; border-radius: 4px; margin: 4px; padding: 4px;\">")
                    .replace("</html>", "</div>"));
            markdownContent.append("\n\n");
        }
        content.setDocument(new MarkdownDocument(markdownContent.toString()));

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(algorithms.size() + " algorithms", UIUtils.getIconFromResources("actions/edit-select-all.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(algorithms),
                canvas.getContextActions(),
                canvas);

        add(toolBar, BorderLayout.NORTH);
    }
}
