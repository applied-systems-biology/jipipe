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

package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panel shown when multiple algorithms are selected
 */
public class ACAQJsonAlgorithmExporterMultiSelectionPanelUI extends ACAQWorkbenchPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithmGraphCanvasUI canvas;
    private Set<ACAQGraphNode> algorithms;

    /**
     * @param workbenchUI Workbench UI
     * @param canvas      The graph
     * @param algorithms  Selected algorithms
     */
    public ACAQJsonAlgorithmExporterMultiSelectionPanelUI(ACAQWorkbench workbenchUI, ACAQAlgorithmGraphCanvasUI canvas, Set<ACAQGraphNode> algorithms) {
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

        if (canvas.getCopyPasteBehavior() != null) {
            JButton cutButton = new JButton(UIUtils.getIconFromResources("cut.png"));
            cutButton.setToolTipText("Cut");
            cutButton.setEnabled(algorithms.stream().allMatch(algorithm -> graph.canUserDelete(algorithm)));
            cutButton.addActionListener(e -> canvas.getCopyPasteBehavior().cut(algorithms));
            toolBar.add(cutButton);

            JButton copyButton = new JButton(UIUtils.getIconFromResources("copy.png"));
            copyButton.setToolTipText("Copy");
            copyButton.addActionListener(e -> canvas.getCopyPasteBehavior().copy(algorithms));
            toolBar.add(copyButton);
        }

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete algorithms");
        deleteButton.setEnabled(algorithms.stream().allMatch(algorithm -> graph.canUserDelete(algorithm)));
        deleteButton.addActionListener(e -> deleteAlgorithms());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void deleteAlgorithms() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the following algorithms: " +
                        algorithms.stream().map(a -> "'" + a.getName() + "'").collect(Collectors.joining(", "))
                        + "?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (ACAQGraphNode algorithm : algorithms) {
                graph.removeNode(algorithm);
            }
        }
    }
}
