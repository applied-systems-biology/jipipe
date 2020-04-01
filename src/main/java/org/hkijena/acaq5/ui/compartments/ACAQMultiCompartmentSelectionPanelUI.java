package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.ACAQProjectUIPanel;
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
 * UI when multiple {@link ACAQProjectCompartment} instances are selected
 */
public class ACAQMultiCompartmentSelectionPanelUI extends ACAQProjectUIPanel {
    private Set<ACAQProjectCompartment> compartments;

    /**
     * @param workbenchUI  The workbench UI
     * @param compartments The compartment selection
     */
    public ACAQMultiCompartmentSelectionPanelUI(ACAQProjectUI workbenchUI, Set<ACAQProjectCompartment> compartments) {
        super(workbenchUI);
        this.compartments = compartments;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader content = new MarkdownReader(false);
        add(content, BorderLayout.CENTER);

        StringBuilder markdownContent = new StringBuilder();
        for (ACAQProjectCompartment compartment : compartments.stream().sorted(Comparator.comparing(ACAQAlgorithm::getName)).collect(Collectors.toList())) {
            markdownContent.append(TooltipUtils.getProjectCompartmentTooltip(compartment, getProject().getGraph())
                    .replace("<html>", "<div style=\"border: 1px solid gray; border-radius: 4px; margin: 4px; padding: 4px;\">")
                    .replace("</html>", "</div>"));
            markdownContent.append("\n\n");
        }
        content.setDocument(new MarkdownDocument(markdownContent.toString()));
        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        JLabel nameLabel = new JLabel(compartments.size() + " compartments", UIUtils.getIconFromResources("select.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("edit.png"));
        openButton.addActionListener(e -> openInEditor());
        toolBar.add(openButton);

        JButton deleteButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        deleteButton.setToolTipText("Delete compartments");
        deleteButton.addActionListener(e -> deleteCompartments());
        toolBar.add(deleteButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void openInEditor() {
        for (ACAQProjectCompartment compartment : compartments) {
            getWorkbenchUI().openCompartmentGraph(compartment, true);
        }
    }

    private void deleteCompartments() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the compartments: " +
                compartments.stream().map(c -> "'" + c.getName() + "'").collect(Collectors.joining(", ")) + "?\n" +
                "You will lose all nodes stored in those compartments.", "Delete compartments", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            for (ACAQProjectCompartment compartment : compartments) {
                compartment.getProject().removeCompartment(compartment);
            }
        }
    }
}
