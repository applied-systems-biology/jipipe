package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.ACAQExportedCompartment;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQAlgorithmParametersUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSlotEditorUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class ACAQMultiCompartmentSelectionPanelUI extends ACAQUIPanel {
    private Set<ACAQProjectCompartment> compartments;

    public ACAQMultiCompartmentSelectionPanelUI(ACAQWorkbenchUI workbenchUI, Set<ACAQProjectCompartment> compartments) {
        super(workbenchUI);
        this.compartments = compartments;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
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
