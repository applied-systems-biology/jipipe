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

package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UI when multiple algorithms are selected
 */
public class ACAQMultiAlgorithmSelectionPanelUI extends ACAQProjectWorkbenchPanel {
    private ACAQGraph graph;
    private ACAQAlgorithmGraphCanvasUI canvas;
    private Set<ACAQGraphNode> algorithms;

    /**
     * @param workbenchUI The workbench
     * @param canvas      The algorithm graph
     * @param algorithms  The algorithm selection
     */
    public ACAQMultiAlgorithmSelectionPanelUI(ACAQProjectWorkbench workbenchUI, ACAQAlgorithmGraphCanvasUI canvas, Set<ACAQGraphNode> algorithms) {
        super(workbenchUI);
        this.graph = canvas.getAlgorithmGraph();
        this.canvas = canvas;
        this.algorithms = algorithms;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader content = new MarkdownReader(false);
        add(content, BorderLayout.CENTER);

        StringBuilder markdownContent = new StringBuilder();
        for (ACAQGraphNode algorithm : algorithms.stream().sorted(Comparator.comparing(ACAQGraphNode::getName)).collect(Collectors.toList())) {
            markdownContent.append(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration())
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
        JLabel nameLabel = new JLabel(algorithms.size() + " algorithms", UIUtils.getIconFromResources("select.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        ACAQAlgorithmGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(algorithms),
                canvas.getContextActions(),
                canvas);

        add(toolBar, BorderLayout.NORTH);
    }
}
