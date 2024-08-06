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

package org.hkijena.jipipe.desktop.app.grapheditor.compartments.properties;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanelImageComponent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopCompartmentsQuickGuidePanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopCompartmentsGraphEditorUI graphEditorUI;

    public JIPipeDesktopCompartmentsQuickGuidePanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipeDesktopCompartmentsGraphEditorUI graphEditorUI) {
        super(desktopWorkbench);
        this.graphEditorUI = graphEditorUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(UIUtils.createInfoLabel("Quick guide", "Find more detailed tutorials on jipipe.org"));
        add(toolBar, BorderLayout.NORTH);

        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        formPanel.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(formPanel, BorderLayout.CENTER);

        // Adding new nodes
        JIPipeDesktopFormPanel.GroupHeaderPanel addingNewNodesHeader = formPanel.addGroupHeader("Adding new compartments", UIUtils.getIconFromResources("actions/polygon-add-nodes.png"));
//        JButton goToAddNodesPanelButton = UIUtils.createButton("Go to", UIUtils.getIconFromResources("actions/go-jump.png"), () -> graphEditorUI.getDockPanel().activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_ADD_NODES, true));
//        UIUtils.makeButtonHighlightedSuccess(goToAddNodesPanelButton);
//        addingNewNodesHeader.addToTitlePanel(goToAddNodesPanelButton);
        formPanel.addWideToForm(new JLabel("Create compartments from the 'Add compartments' panel", UIUtils.getIconFromResources("actions/add.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Double-click compartments to edit their contents", UIUtils.getIconFromResources("actions/arrow-pointer.png"), JLabel.LEFT));
//        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-create-nodes.png"), true));

        // Navigation
        formPanel.addGroupHeader("Navigation", UIUtils.getIconFromResources("actions/document-preview-archive.png"));
        formPanel.addWideToForm(new JLabel("Drag nodes by their colored area to move them", UIUtils.getIconFromResources("actions/transform-move.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Select nodes by dragging a rectangle around them", UIUtils.getIconFromResources("actions/selection-touch.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Alternative: hold the Shift key and click nodes", UIUtils.getIconFromResources("actions/keyboard.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Hold Ctrl and scroll to zoom", UIUtils.getIconFromResources("actions/magnifying-glass.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Alternative: zoom controls can be found at the top right", UIUtils.getIconFromResources("actions/interface.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-navigation.png"), true));

        // Creating edges
        formPanel.addGroupHeader("Connecting compartments", UIUtils.getIconFromResources("actions/connector-avoid.png"));
        formPanel.addWideToForm(new JLabel("Click the + button to add a compartment output slot", UIUtils.getIconFromResources("actions/add.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Drag a line between two slots to create a connection", UIUtils.getIconFromResources("actions/input-mouse-click-left.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Drag a line between connected slots to disconnect slots", UIUtils.getIconFromResources("actions/gtk-disconnect.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Right-click slots for settings and information", UIUtils.getIconFromResources("actions/arrow-pointer.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-edges.png"), true));

        // Running
        formPanel.addGroupHeader("Executing workflows", UIUtils.getIconFromResources("actions/run-build.png"));
        formPanel.addWideToForm(new JLabel("Click the play button to run a specific compartment", UIUtils.getIconFromResources("actions/graph-node.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JLabel("Run the whole project with the 'Run' command", UIUtils.getIconFromResources("actions/run-build.png"), JLabel.LEFT));
        formPanel.addWideToForm(new JIPipeDesktopFormPanelImageComponent(UIUtils.getImageFromResources("documentation/graph-editor-overview-running.png"), true));


        formPanel.addVerticalGlue();
    }
}
