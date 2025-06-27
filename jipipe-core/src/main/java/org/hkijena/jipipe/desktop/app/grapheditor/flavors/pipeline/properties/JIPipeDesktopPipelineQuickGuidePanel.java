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

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.properties;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanelImageComponent;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.JIPipe;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopPipelineQuickGuidePanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopPipelineGraphEditorUI graphEditorUI;

    public JIPipeDesktopPipelineQuickGuidePanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipeDesktopPipelineGraphEditorUI graphEditorUI) {
        super(desktopWorkbench);
        this.graphEditorUI = graphEditorUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(UIUtils.createInfoLabel("Quick guide", "Find more detailed tutorials on jipipe.org"));
        JButton goToAddNodesPanelButton = UIUtils.createButton("Start adding nodes", JIPipe.RESOURCES.getIcon16("actions/polygon-add-nodes.png"), () -> graphEditorUI.getDockPanel().activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_ADD_NODES, true));
        UIUtils.makeButtonHighlightedSuccess(goToAddNodesPanelButton);
        toolBar.add(goToAddNodesPanelButton);
        add(toolBar, BorderLayout.NORTH);

        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        formPanel.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(formPanel, BorderLayout.CENTER);

        // Adding new nodes
        formPanel.addGroupHeader("Adding new nodes", JIPipe.RESOURCES.getIcon16("actions/polygon-add-nodes.png"));
        formPanel.addWideToForm(new JLabel("Drag nodes from the 'Add nodes' panel into the workflow", JIPipe.RESOURCES.getIcon16("actions/arrow-pointer.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("You can also double-click items", JIPipe.RESOURCES.getIcon16("actions/input-mouse-click-left.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Right-click categories for the classic menu", JIPipe.RESOURCES.getIcon16("actions/input-mouse-click-right.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(JIPipe.RESOURCES.getVariantResourceAsImage("documentation/graph-editor-overview-create-nodes.png"), true));

        // Navigation
        formPanel.addGroupHeader("Navigation", JIPipe.RESOURCES.getIcon16("actions/document-preview-archive.png"));
        formPanel.addWideToForm(new JLabel("Drag nodes by their colored area to move them", JIPipe.RESOURCES.getIcon16("actions/transform-move.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Select nodes by dragging a rectangle around them", JIPipe.RESOURCES.getIcon16("actions/selection-touch.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Alternative: hold the Shift key and click nodes", JIPipe.RESOURCES.getIcon16("actions/keyboard.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Hold Ctrl and scroll to zoom", JIPipe.RESOURCES.getIcon16("actions/magnifying-glass.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Alternative: zoom controls can be found at the top right", JIPipe.RESOURCES.getIcon16("actions/interface.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(JIPipe.RESOURCES.getVariantResourceAsImage("documentation/graph-editor-overview-navigation.png"), true));

        // Creating edges
        formPanel.addGroupHeader("Creating workflows", JIPipe.RESOURCES.getIcon16("actions/connector-avoid.png"));
        formPanel.addWideToForm(new JLabel("Drag a line between two slots to create a connection", JIPipe.RESOURCES.getIcon16("actions/input-mouse-click-left.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Drag a line between connected slots to disconnect slots", JIPipe.RESOURCES.getIcon16("actions/gtk-disconnect.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Right-click slots for settings and information", JIPipe.RESOURCES.getIcon16("actions/arrow-pointer.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(JIPipe.RESOURCES.getVariantResourceAsImage("documentation/graph-editor-overview-edges.png"), true));

        // Running
        formPanel.addGroupHeader("Executing workflows", JIPipe.RESOURCES.getIcon16("actions/run-build.png"));
        formPanel.addWideToForm(new JLabel("Click the play button to run a specific node", JIPipe.RESOURCES.getIcon16("actions/graph-node.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Run the whole project with the 'Run' command", JIPipe.RESOURCES.getIcon16("actions/run-build.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(JIPipe.RESOURCES.getVariantResourceAsImage("documentation/graph-editor-overview-running.png"), true));


        formPanel.addVerticalGlue();
    }
}
