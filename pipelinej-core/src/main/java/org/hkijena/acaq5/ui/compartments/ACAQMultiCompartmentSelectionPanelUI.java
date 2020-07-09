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

package org.hkijena.acaq5.ui.compartments;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphEditorUI;
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
public class ACAQMultiCompartmentSelectionPanelUI extends ACAQProjectWorkbenchPanel {
    private final ACAQGraphCanvasUI canvas;
    private Set<ACAQProjectCompartment> compartments;

    /**
     * @param workbenchUI  The workbench UI
     * @param compartments The compartment selection
     * @param canvas       the graph canvas
     */
    public ACAQMultiCompartmentSelectionPanelUI(ACAQProjectWorkbench workbenchUI, Set<ACAQProjectCompartment> compartments, ACAQGraphCanvasUI canvas) {
        super(workbenchUI);
        this.compartments = compartments;
        this.canvas = canvas;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader content = new MarkdownReader(false);
        add(content, BorderLayout.CENTER);

        StringBuilder markdownContent = new StringBuilder();
        for (ACAQProjectCompartment compartment : compartments.stream().sorted(Comparator.comparing(ACAQGraphNode::getName)).collect(Collectors.toList())) {
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
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(compartments.size() + " compartments", UIUtils.getIconFromResources("select.png"), JLabel.LEFT);
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        ACAQGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(compartments),
                canvas.getContextActions(),
                canvas);

        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("edit.png"));
        openButton.addActionListener(e -> openInEditor());
        toolBar.add(openButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void openInEditor() {
        for (ACAQProjectCompartment compartment : compartments) {
            getProjectWorkbench().openCompartmentGraph(compartment, true);
        }
    }
}
