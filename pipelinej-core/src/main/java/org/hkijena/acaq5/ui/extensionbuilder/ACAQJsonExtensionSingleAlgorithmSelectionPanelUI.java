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

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphEditorUI;
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
public class ACAQJsonExtensionSingleAlgorithmSelectionPanelUI extends ACAQJsonExtensionWorkbenchPanel {
    private final ACAQGraphCanvasUI canvas;
    private final ACAQGraphEditorUI graphEditorUI;
    private final ACAQGraphNode algorithm;

    /**
     * @param graphEditorUI the graph editor
     * @param algorithm     The algorithm
     */
    public ACAQJsonExtensionSingleAlgorithmSelectionPanelUI(ACAQGraphEditorUI graphEditorUI, ACAQGraphNode algorithm) {
        super((ACAQJsonExtensionWorkbench) graphEditorUI.getWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.canvas = graphEditorUI.getCanvasUI();
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane();

        ParameterPanel parametersUI = new ParameterPanel(getExtensionWorkbenchUI(),
                algorithm,
                TooltipUtils.getAlgorithmDocumentation(algorithm.getDeclaration()),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("cog.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        ACAQSlotEditorUI slotEditorUI = new ACAQSlotEditorUI(graphEditorUI, algorithm);
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

        ACAQGraphEditorUI.installContextActionsInto(toolBar,
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
