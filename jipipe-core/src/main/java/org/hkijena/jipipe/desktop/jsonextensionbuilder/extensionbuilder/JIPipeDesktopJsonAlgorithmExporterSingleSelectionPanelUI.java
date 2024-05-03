/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphNodeSlotEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidJIPipeDesktopColorIcon;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * Shown when one algorithm is selected
 */
public class JIPipeDesktopJsonAlgorithmExporterSingleSelectionPanelUI extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final JIPipeGraphNode node;
    private JIPipeDesktopGraphCanvasUI canvas;

    /**
     * @param graphEditorUI the graph editor
     * @param node          The algorithm
     */
    public JIPipeDesktopJsonAlgorithmExporterSingleSelectionPanelUI(JIPipeDesktopGraphEditorUI graphEditorUI, JIPipeGraphNode node) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.canvas = graphEditorUI.getCanvasUI();
        this.node = node;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopTabPane tabbedPane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Top);

        JIPipeDesktopParameterPanel parametersUI = new JIPipeDesktopParameterPanel(getDesktopWorkbench(),
                node,
                TooltipUtils.getAlgorithmDocumentation(node.getInfo()),
                JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.DOCUMENTATION_BELOW | JIPipeDesktopParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("actions/configure.png"),
                parametersUI,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);

        JIPipeDesktopGraphNodeSlotEditorUI slotEditorUI = new JIPipeDesktopGraphNodeSlotEditorUI(graphEditorUI, node);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("actions/plug.png"),
                slotEditorUI,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);

        if (node instanceof JIPipeIterationStepAlgorithm) {
            tabbedPane.addTab("Input management",
                    UIUtils.getIconFromResources("actions/package.png"),
                    new JIPipeDesktopParameterPanel(getDesktopWorkbench(), ((JIPipeIterationStepAlgorithm) node).getGenerationSettingsInterface(), null, JIPipeDesktopParameterPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterPanel.WITH_SCROLLING),
                    JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        }

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(node.getName(), new SolidJIPipeDesktopColorIcon(16, 16, UIUtils.getFillColorFor(node.getInfo())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(node.getInfo()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeDesktopGraphEditorUI.installContextActionsInto(toolBar,
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
