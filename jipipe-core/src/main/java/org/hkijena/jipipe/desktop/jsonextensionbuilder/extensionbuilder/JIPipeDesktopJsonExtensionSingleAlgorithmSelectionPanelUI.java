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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphNodeSlotEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidJIPipeDesktopColorIcon;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbench;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbenchPanel;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * Shown when one algorithm is selected
 */
public class JIPipeDesktopJsonExtensionSingleAlgorithmSelectionPanelUI extends JIPipeDesktopJsonExtensionWorkbenchPanel {
    private final JIPipeDesktopGraphCanvasUI canvas;
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final JIPipeGraphNode algorithm;

    /**
     * @param graphEditorUI the graph editor
     * @param algorithm     The algorithm
     */
    public JIPipeDesktopJsonExtensionSingleAlgorithmSelectionPanelUI(JIPipeDesktopGraphEditorUI graphEditorUI, JIPipeGraphNode algorithm) {
        super((JIPipeDesktopJsonExtensionWorkbench) graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.canvas = graphEditorUI.getCanvasUI();
        this.algorithm = algorithm;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopTabPane tabbedPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);

        JIPipeDesktopParameterPanel parametersUI = new JIPipeDesktopParameterPanel(getExtensionWorkbenchUI(),
                algorithm,
                TooltipUtils.getAlgorithmDocumentation(algorithm.getInfo()),
                JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.DOCUMENTATION_BELOW | JIPipeDesktopParameterPanel.WITH_SEARCH_BAR);
        tabbedPane.addTab("Parameters", UIUtils.getIconFromResources("actions/configure.png"),
                parametersUI,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);

        JIPipeDesktopGraphNodeSlotEditorUI slotEditorUI = new JIPipeDesktopGraphNodeSlotEditorUI(graphEditorUI, algorithm);
        tabbedPane.addTab("Slots", UIUtils.getIconFromResources("actions/database.png"),
                slotEditorUI,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabbedPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JLabel nameLabel = new JLabel(algorithm.getName(), new SolidJIPipeDesktopColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getInfo())), JLabel.LEFT);
        nameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getInfo()));
        toolBar.add(nameLabel);

        toolBar.add(Box.createHorizontalGlue());

        JIPipeDesktopGraphEditorUI.installContextActionsInto(toolBar,
                canvas.getNodeUIsFor(Collections.singleton(algorithm)),
                canvas.getContextActions(),
                canvas);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * @return The algorithm
     */
    public JIPipeGraphNode getAlgorithm() {
        return algorithm;
    }
}
