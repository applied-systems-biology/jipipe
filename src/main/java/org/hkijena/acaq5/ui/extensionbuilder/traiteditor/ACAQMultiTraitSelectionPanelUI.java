package org.hkijena.acaq5.ui.extensionbuilder.traiteditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitGraph;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitNode;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UI when multiple {@link ACAQTraitNode} are selected
 */
public class ACAQMultiTraitSelectionPanelUI extends ACAQJsonExtensionWorkbenchPanel {
    private Set<ACAQTraitNode> algorithms;
    private ACAQTraitGraph graph;

    /**
     * @param workbenchUI The workbench
     * @param graph       The trait graph
     * @param algorithms  The selected trait nodes
     */
    public ACAQMultiTraitSelectionPanelUI(ACAQJsonExtensionWorkbench workbenchUI, ACAQTraitGraph graph, Set<ACAQTraitNode> algorithms) {
        super(workbenchUI);
        this.graph = graph;
        this.algorithms = algorithms;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader content = new MarkdownReader(false);
        add(content, BorderLayout.CENTER);

        StringBuilder markdownContent = new StringBuilder();
        for (ACAQTraitNode node : algorithms.stream().sorted(Comparator.comparing(ACAQAlgorithm::getName)).collect(Collectors.toList())) {
            markdownContent.append(TooltipUtils.getTraitTooltip(node.getTraitDeclaration())
                    .replace("<html>", "<div style=\"border: 1px solid gray; border-radius: 4px; margin: 4px; padding: 4px;\">")
                    .replace("</html>", "</div>"));
            markdownContent.append("\n\n");
        }
        content.setDocument(new MarkdownDocument(markdownContent.toString()));
        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        JLabel nameLabel = new JLabel(algorithms.size() + " annotation types", UIUtils.getIconFromResources("select.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete annotations");
        deleteButton.addActionListener(e -> deleteTraits());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }


    private void deleteTraits() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the annotations: " +
                        algorithms.stream().map(c -> "'" + c.getName() + "'").collect(Collectors.joining(", ")) + "?\n",
                "Delete annotations", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            for (ACAQTraitNode node : algorithms) {
                graph.removeNode(node);
            }
        }
    }
}
