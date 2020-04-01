package org.hkijena.acaq5.ui.extensionbuilder.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUIPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
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
public class ACAQJsonExtensionMultiAlgorithmSelectionPanelUI extends ACAQJsonExtensionUIPanel {
    private ACAQAlgorithmGraph graph;
    private Set<ACAQAlgorithm> algorithms;

    /**
     * @param workbenchUI Workbench UI
     * @param graph       The graph
     * @param algorithms  Selected algorithms
     */
    public ACAQJsonExtensionMultiAlgorithmSelectionPanelUI(ACAQJsonExtensionUI workbenchUI, ACAQAlgorithmGraph graph, Set<ACAQAlgorithm> algorithms) {
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
        for (ACAQAlgorithm algorithm : algorithms.stream().sorted(Comparator.comparing(ACAQAlgorithm::getName)).collect(Collectors.toList())) {
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
        JLabel nameLabel = new JLabel(algorithms.size() + " algorithms", UIUtils.getIconFromResources("select.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

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
            for (ACAQAlgorithm algorithm : algorithms) {
                graph.removeNode(algorithm);
            }
        }
    }
}
