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

package org.hkijena.jipipe.desktop.app.grapheditor.groups;

import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor for a {@link JIPipeNodeGroup}
 * Contains a {@link JIPipePipelineGraphEditorUI} instance that allows editing the compartment's content
 */
public class JIPipeDesktopNodeGroupUI extends JIPipeDesktopWorkbenchPanel implements Disposable {

    private JIPipeNodeGroup nodeGroup;
    private JIPipePipelineGraphEditorUI graphUI;

    /**
     * Creates a new editor
     *
     * @param workbenchUI the workbench UI
     * @param nodeGroup   the compartment
     */
    public JIPipeDesktopNodeGroupUI(JIPipeDesktopWorkbench workbenchUI, JIPipeNodeGroup nodeGroup) {
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
    public static void openGroupNodeGraph(JIPipeDesktopWorkbench workbench, JIPipeNodeGroup nodeGroup, boolean switchToTab) {
        List<JIPipeDesktopNodeGroupUI> compartmentUIs = findNodeGraphUIs(workbench, nodeGroup);
        if (compartmentUIs.isEmpty()) {
            JIPipeDesktopNodeGroupUI compartmentUI = new JIPipeDesktopNodeGroupUI(workbench, nodeGroup);
            JIPipeDesktopTabPane.DocumentTab documentTab = workbench.getDocumentTabPane().addTab(nodeGroup.getName(),
                    UIUtils.getIconFromResources("actions/object-group.png"),
                    compartmentUI,
                    JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                    false);
            if (switchToTab)
                workbench.getDocumentTabPane().switchToLastTab();
        } else if (switchToTab) {
            workbench.getDocumentTabPane().switchToContent(compartmentUIs.get(0));
        }
    }

    /**
     * Finds open tabs
     *
     * @param nodeGroup Targeted compartment
     * @return List of UIs
     */
    public static List<JIPipeDesktopNodeGroupUI> findNodeGraphUIs(JIPipeDesktopWorkbench workbench, JIPipeNodeGroup nodeGroup) {
        List<JIPipeDesktopNodeGroupUI> result = new ArrayList<>();
        for (JIPipeDesktopTabPane.DocumentTab tab : workbench.getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeDesktopNodeGroupUI) {
                if (((JIPipeDesktopNodeGroupUI) tab.getContent()).getNodeGroup() == nodeGroup)
                    result.add((JIPipeDesktopNodeGroupUI) tab.getContent());
            }
        }
        return result;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        graphUI = new JIPipePipelineGraphEditorUI(getDesktopWorkbench(), nodeGroup.getWrappedGraph(), null);
        add(graphUI, BorderLayout.CENTER);
    }


    @Override
    public void dispose() {
        Disposable.super.dispose();
        graphUI.dispose();
    }

    /**
     * @return The displayed compartment
     */
    public JIPipeNodeGroup getNodeGroup() {
        return nodeGroup;
    }
}
