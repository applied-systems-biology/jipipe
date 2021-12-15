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

package org.hkijena.jipipe.ui.grapheditor.compartments;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.settings.JIPipeSlotEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.HashMap;

/**
 * UI for a single {@link JIPipeProjectCompartment}
 */
public class JIPipeSingleCompartmentSelectionPanelUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeProjectCompartment compartment;
    private final JIPipeGraphCanvasUI canvas;
    private final JIPipeGraphEditorUI graphEditorUI;

    /**
     * @param graphEditorUI the graph editor
     * @param compartment   the compartment
     */
    public JIPipeSingleCompartmentSelectionPanelUI(JIPipeGraphEditorUI graphEditorUI, JIPipeProjectCompartment compartment) {
        super((JIPipeProjectWorkbench) graphEditorUI.getWorkbench());
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
                MarkdownDocument.fromPluginResource("documentation/compartment-graph.md", new HashMap<>()),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SCROLLING);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("actions/configure.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        JIPipeSlotEditorUI compartmentSlotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, compartment);
        tabbedPane.addTab("Connections", UIUtils.getIconFromResources("data-types/graph-compartment.png"),
                compartmentSlotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        JIPipeSlotEditorUI slotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, compartment.getOutputNode());
        tabbedPane.addTab("Output data", UIUtils.getIconFromResources("actions/database.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(compartment.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(compartment.getInfo())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getProjectCompartmentTooltip(compartment, getProject().getGraph()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(compartment)),
                canvas.getContextActions(),
                canvas);

        JButton openButton = new JButton("Open in editor", UIUtils.getIconFromResources("actions/edit.png"));
        openButton.addActionListener(e -> openInEditor());
        toolBar.add(openButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void openInEditor() {
        getProjectWorkbench().getOrOpenPipelineEditorTab(compartment, true);
    }

    /**
     * @return the compartment
     */
    public JIPipeGraphNode getCompartment() {
        return compartment;
    }
}
