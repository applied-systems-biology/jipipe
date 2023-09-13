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

package org.hkijena.jipipe.ui.extensionbuilder;

import org.hkijena.jipipe.api.nodes.JIPipeDataBatchAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.properties.JIPipeSlotEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * Shown when one algorithm is selected
 */
public class JIPipeJsonAlgorithmExporterSingleSelectionPanelUI extends JIPipeWorkbenchPanel {
    private final JIPipeGraphEditorUI graphEditorUI;
    private final JIPipeGraphNode node;
    private JIPipeGraphCanvasUI canvas;

    /**
     * @param graphEditorUI the graph editor
     * @param node          The algorithm
     */
    public JIPipeJsonAlgorithmExporterSingleSelectionPanelUI(JIPipeGraphEditorUI graphEditorUI, JIPipeGraphNode node) {
        super(graphEditorUI.getWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.canvas = graphEditorUI.getCanvasUI();
        this.node = node;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabbedPane = new DocumentTabPane(false);

        ParameterPanel parametersUI = new ParameterPanel(getWorkbench(),
                node,
                TooltipUtils.getAlgorithmDocumentation(node.getInfo()),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("actions/configure.png"),
                parametersUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        JIPipeSlotEditorUI slotEditorUI = new JIPipeSlotEditorUI(graphEditorUI, node);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("actions/plug.png"),
                slotEditorUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        if (node instanceof JIPipeDataBatchAlgorithm) {
            tabbedPane.addTab("Data batches",
                    UIUtils.getIconFromResources("actions/package.png"),
                    new ParameterPanel(getWorkbench(), ((JIPipeDataBatchAlgorithm) node).getGenerationSettingsInterface(), null, ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING),
                    DocumentTabPane.CloseMode.withoutCloseButton);
        }

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(node.getName(), new SolidColorIcon(16, 16, UIUtils.getFillColorFor(node.getInfo())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(node.getInfo()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(node)),
                canvas.getContextActions(),
                canvas);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * @return The algorithm
     */
    public JIPipeGraphNode getNode() {
        return node;
    }
}
