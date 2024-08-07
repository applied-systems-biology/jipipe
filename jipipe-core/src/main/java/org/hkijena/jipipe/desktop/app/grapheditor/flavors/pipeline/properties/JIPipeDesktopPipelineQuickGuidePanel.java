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

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanelImageComponent;
import org.hkijena.jipipe.utils.UIUtils;

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
        JButton goToAddNodesPanelButton = UIUtils.createButton("Start adding nodes", UIUtils.getIconFromResources("actions/polygon-add-nodes.png"), () -> graphEditorUI.getDockPanel().activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_ADD_NODES, true));
        UIUtils.makeButtonHighlightedSuccess(goToAddNodesPanelButton);
        toolBar.add(goToAddNodesPanelButton);
        add(toolBar, BorderLayout.NORTH);

        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        formPanel.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(formPanel, BorderLayout.CENTER);

        // Adding new nodes
        formPanel.addGroupHeader("Adding new nodes", UIUtils.getIconFromResources("actions/polygon-add-nodes.png"));
        formPanel.addWideToForm(new JLabel("Drag nodes from the 'Add nodes' panel into the workflow", UIUtils.getIconFromResources("actions/arrow-pointer.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("You can also double-click items", UIUtils.getIconFromResources("actions/input-mouse-click-left.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Right-click categories for the classic menu", UIUtils.getIconFromResources("actions/input-mouse-click-right.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-create-nodes.png"), true));

        // Navigation
        formPanel.addGroupHeader("Navigation", UIUtils.getIconFromResources("actions/document-preview-archive.png"));
        formPanel.addWideToForm(new JLabel("Drag nodes by their colored area to move them", UIUtils.getIconFromResources("actions/transform-move.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Select nodes by dragging a rectangle around them", UIUtils.getIconFromResources("actions/selection-touch.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Alternative: hold the Shift key and click nodes", UIUtils.getIconFromResources("actions/keyboard.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Hold Ctrl and scroll to zoom", UIUtils.getIconFromResources("actions/magnifying-glass.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Alternative: zoom controls can be found at the top right", UIUtils.getIconFromResources("actions/interface.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-navigation.png"), true));

        // Creating edges
        formPanel.addGroupHeader("Creating workflows", UIUtils.getIconFromResources("actions/connector-avoid.png"));
        formPanel.addWideToForm(new JLabel("Drag a line between two slots to create a connection", UIUtils.getIconFromResources("actions/input-mouse-click-left.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Drag a line between connected slots to disconnect slots", UIUtils.getIconFromResources("actions/gtk-disconnect.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Right-click slots for settings and information", UIUtils.getIconFromResources("actions/arrow-pointer.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-edges.png"), true));

        // Running
        formPanel.addGroupHeader("Executing workflows", UIUtils.getIconFromResources("actions/run-build.png"));
        formPanel.addWideToForm(new JLabel("Click the play button to run a specific node", UIUtils.getIconFromResources("actions/graph-node.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Run the whole project with the 'Run' command", UIUtils.getIconFromResources("actions/run-build.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-running.png"), true));


        formPanel.addVerticalGlue();
    }
}
