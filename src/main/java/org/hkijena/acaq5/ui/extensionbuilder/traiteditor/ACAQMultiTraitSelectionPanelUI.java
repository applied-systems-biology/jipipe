package org.hkijena.acaq5.ui.extensionbuilder.traiteditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUIPanel;
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

public class ACAQMultiTraitSelectionPanelUI extends ACAQJsonExtensionUIPanel {
    private Set<ACAQTraitNode> algorithms;
    private ACAQTraitGraph graph;

    public ACAQMultiTraitSelectionPanelUI(ACAQJsonExtensionUI workbenchUI, ACAQTraitGraph graph, Set<ACAQTraitNode> algorithms) {
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
        JLabel nameLabel = new JLabel(algorithms.size() + " compartments", UIUtils.getIconFromResources("select.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete annotations");
        deleteButton.addActionListener(e -> deleteTraits());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }


    private void deleteTraits() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the compartments: " +
                algorithms.stream().map(c -> "'" + c.getName() + "'").collect(Collectors.joining(", ")) + "?\n" +
                "You will lose all nodes stored in those compartments.", "Delete compartments", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            for (ACAQTraitNode node : algorithms) {
                graph.removeNode(node);
            }
        }
    }
}
