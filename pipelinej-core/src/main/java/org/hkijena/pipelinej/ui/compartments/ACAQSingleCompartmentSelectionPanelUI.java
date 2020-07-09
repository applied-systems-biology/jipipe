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

package org.hkijena.pipelinej.ui.compartments;

import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.pipelinej.ui.components.ColorIcon;
import org.hkijena.pipelinej.ui.components.DocumentTabPane;
import org.hkijena.pipelinej.ui.components.MarkdownDocument;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphEditorUI;
import org.hkijena.pipelinej.ui.grapheditor.settings.ACAQSlotEditorUI;
import org.hkijena.pipelinej.ui.parameters.ParameterPanel;
import org.hkijena.pipelinej.utils.TooltipUtils;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * UI for a single {@link ACAQProjectCompartment}
 */
public class ACAQSingleCompartmentSelectionPanelUI extends ACAQProjectWorkbenchPanel {
    private final ACAQProjectCompartment compartment;
    private final ACAQGraphCanvasUI canvas;
    private final ACAQGraphEditorUI graphEditorUI;

    /**
     * @param graphEditorUI the graph editor
     * @param compartment   the compartment
     */
    public ACAQSingleCompartmentSelectionPanelUI(ACAQGraphEditorUI graphEditorUI, ACAQProjectCompartment compartment) {
        super((ACAQProjectWorkbench) graphEditorUI.getWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.compartment = compartment;
        this.canvas = graphEditorUI.getCanvasUI();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getProjectWorkbench(),
                compartment,
                MarkdownDocument.fromPluginResource("documentation/compartment-graph.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SCROLLING);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI compartmentSlotEditorUI = new ACAQSlotEditorUI(graphEditorUI, compartment);
        tabbedPane.addTab("Connections", UIUtils.getIconFromResources("graph-compartment.png"),
                compartmentSlotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(graphEditorUI, compartment.getOutputNode());
        tabbedPane.addTab("Output data", UIUtils.getIconFromResources("database.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(compartment.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(compartment.getDeclaration())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getProjectCompartmentTooltip(compartment, getProject().getGraph()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        ACAQGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(compartment)),
                canvas.getContextActions(),
                canvas);

        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("edit.png"));
        openButton.addActionListener(e -> openInEditor());
        toolBar.add(openButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void openInEditor() {
        getProjectWorkbench().openCompartmentGraph(compartment, true);
    }

    /**
     * @return the compartment
     */
    public ACAQGraphNode getCompartment() {
        return compartment;
    }
}
