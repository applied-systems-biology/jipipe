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

package org.hkijena.acaq5.ui.grouping;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.compartments.ACAQCompartmentUI;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCompartmentUI;
import org.hkijena.acaq5.utils.UIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor for a {@link org.hkijena.acaq5.api.grouping.NodeGroup}
 * Contains a {@link ACAQAlgorithmGraphCompartmentUI} instance that allows editing the compartment's content
 */
public class ACAQNodeGroupUI extends ACAQWorkbenchPanel {

    private NodeGroup nodeGroup;
    private ACAQAlgorithmGraphCompartmentUI graphUI;

    /**
     * Creates a new editor
     *
     * @param workbenchUI the workbench UI
     * @param nodeGroup   the compartment
     */
    public ACAQNodeGroupUI(ACAQWorkbench workbenchUI, NodeGroup nodeGroup) {
        super(workbenchUI);
        this.nodeGroup = nodeGroup;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        graphUI = new ACAQAlgorithmGraphCompartmentUI(getWorkbench(), nodeGroup.getWrappedGraph(), ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        add(graphUI, BorderLayout.CENTER);
    }

    /**
     * @return The displayed compartment
     */
    public NodeGroup getNodeGroup() {
        return nodeGroup;
    }

    /**
     * Opens the graph editor for specified compartment
     *
     * @param nodeGroup   The compartment
     * @param switchToTab If true, switch to the tab
     */
    public static void openGroupNodeGraph(ACAQWorkbench workbench, NodeGroup nodeGroup, boolean switchToTab) {
        List<ACAQNodeGroupUI> compartmentUIs = findNodeGraphUIs(workbench, nodeGroup);
        if (compartmentUIs.isEmpty()) {
            ACAQNodeGroupUI compartmentUI = new ACAQNodeGroupUI(workbench, nodeGroup);
            DocumentTabPane.DocumentTab documentTab = workbench.getDocumentTabPane().addTab(nodeGroup.getName(),
                    UIUtils.getIconFromResources("group.png"),
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
     * Finds open {@link ACAQCompartmentUI} tabs
     *
     * @param nodeGroup Targeted compartment
     * @return List of UIs
     */
    public static List<ACAQNodeGroupUI> findNodeGraphUIs(ACAQWorkbench workbench, NodeGroup nodeGroup) {
        List<ACAQNodeGroupUI> result = new ArrayList<>();
        for (DocumentTabPane.DocumentTab tab : workbench.getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof ACAQNodeGroupUI) {
                if (((ACAQNodeGroupUI) tab.getContent()).getNodeGroup() == nodeGroup)
                    result.add((ACAQNodeGroupUI) tab.getContent());
            }
        }
        return result;
    }
}
