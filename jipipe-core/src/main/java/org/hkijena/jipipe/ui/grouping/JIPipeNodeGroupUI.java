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

package org.hkijena.jipipe.ui.grouping;

import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor for a {@link org.hkijena.jipipe.api.grouping.NodeGroup}
 * Contains a {@link JIPipePipelineGraphEditorUI} instance that allows editing the compartment's content
 */
public class JIPipeNodeGroupUI extends JIPipeWorkbenchPanel {

    private NodeGroup nodeGroup;
    private JIPipePipelineGraphEditorUI graphUI;

    /**
     * Creates a new editor
     *
     * @param workbenchUI the workbench UI
     * @param nodeGroup   the compartment
     */
    public JIPipeNodeGroupUI(JIPipeWorkbench workbenchUI, NodeGroup nodeGroup) {
        super(workbenchUI);
        this.nodeGroup = nodeGroup;
        initialize();
    }

    /**
     * Opens the graph editor for specified compartment
     *
     * @param nodeGroup   The compartment
     * @param switchToTab If true, switch to the tab
     */
    public static void openGroupNodeGraph(JIPipeWorkbench workbench, NodeGroup nodeGroup, boolean switchToTab) {
        List<JIPipeNodeGroupUI> compartmentUIs = findNodeGraphUIs(workbench, nodeGroup);
        if (compartmentUIs.isEmpty()) {
            JIPipeNodeGroupUI compartmentUI = new JIPipeNodeGroupUI(workbench, nodeGroup);
            DocumentTabPane.DocumentTab documentTab = workbench.getDocumentTabPane().addTab(nodeGroup.getName(),
                    UIUtils.getIconFromResources("actions/object-group.png"),
                    compartmentUI,
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    false);
            if (switchToTab)
                workbench.getDocumentTabPane().switchToLastTab();
        } else if (switchToTab) {
            workbench.getDocumentTabPane().switchToContent(compartmentUIs.get(0));
        }
    }

    /**
     * Finds open {@link JIPipeNodeGroupUI} tabs
     *
     * @param nodeGroup Targeted compartment
     * @return List of UIs
     */
    public static List<JIPipeNodeGroupUI> findNodeGraphUIs(JIPipeWorkbench workbench, NodeGroup nodeGroup) {
        List<JIPipeNodeGroupUI> result = new ArrayList<>();
        for (DocumentTabPane.DocumentTab tab : workbench.getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeNodeGroupUI) {
                if (((JIPipeNodeGroupUI) tab.getContent()).getNodeGroup() == nodeGroup)
                    result.add((JIPipeNodeGroupUI) tab.getContent());
            }
        }
        return result;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        graphUI = new JIPipePipelineGraphEditorUI(getWorkbench(), nodeGroup.getWrappedGraph(), null);
        add(graphUI, BorderLayout.CENTER);
    }

    /**
     * @return The displayed compartment
     */
    public NodeGroup getNodeGroup() {
        return nodeGroup;
    }
}
