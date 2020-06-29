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
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSlotEditorUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * Shown when one algorithm is selected
 */
public class ACAQJsonAlgorithmExporterSingleSelectionPanelUI extends ACAQWorkbenchPanel {
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithmGraphCanvasUI canvas;
    private ACAQGraphNode algorithm;

    /**
     * @param workbenchUI The workbench UI
     * @param canvas      The algorithm graph
     * @param algorithm   The algorithm
     */
    public ACAQJsonAlgorithmExporterSingleSelectionPanelUI(ACAQWorkbench workbenchUI, ACAQAlgorithmGraphCanvasUI canvas, ACAQGraphNode algorithm) {
        super(workbenchUI);
        this.graph = canvas.getAlgorithmGraph();
        this.canvas = canvas;
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getWorkbench(),
                algorithm,
                TooltipUtils.getAlgorithmDocumentation(algorithm.getDeclaration()),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(algorithm);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("database.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getDeclaration())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        ACAQAlgorithmGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(algorithm)),
                canvas.getContextActions(),
                canvas);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * @return The algorithm
     */
    public ACAQGraphNode getAlgorithm() {
        return algorithm;
    }
}
