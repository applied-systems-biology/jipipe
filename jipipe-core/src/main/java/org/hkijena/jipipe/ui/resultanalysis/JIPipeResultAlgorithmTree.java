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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.resultanalysis.renderers.JIPipeResultTreeCellRenderer;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Displays a tree where the user can select data slots
 */
public class JIPipeResultAlgorithmTree extends JIPipeProjectWorkbenchPanel {
    private final JIPipeProject project;
    private final Path storagePath;
    private JScrollPane treeScrollPane;
    private JTree tree;
    private SearchTextField searchTextField;


    public JIPipeResultAlgorithmTree(JIPipeProjectWorkbench workbenchUI, JIPipeProject project, Path storagePath) {
        super(workbenchUI);
        this.project = project;
        this.storagePath = storagePath;
        initialize();
        refreshTree();
    }

    private void refreshTree() {
        int scrollPosition = treeScrollPane.getVerticalScrollBar().getValue();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
        for (JIPipeProjectCompartment compartment : project.getCompartmentGraph().traverse()
                .stream().filter(a -> a instanceof JIPipeProjectCompartment)
                .map(a -> (JIPipeProjectCompartment) a).collect(Collectors.toList())) {
            DefaultMutableTreeNode compartmentNode = new DefaultMutableTreeNode(compartment);
            boolean compartmentMatches = searchTextField.test(compartment.getName());
            boolean compartmentHasMatchedChildren = false;
            UUID projectCompartmentUUID = compartment.getProjectCompartmentUUID();

            for (JIPipeGraphNode algorithm : project.getGraph().traverse()) {
                if (Objects.equals(algorithm.getCompartmentUUIDInParentGraph(), projectCompartmentUUID)) {
                    DefaultMutableTreeNode algorithmNode = new DefaultMutableTreeNode(algorithm);

                    boolean algorithmMatches = compartmentMatches || searchTextField.test(algorithm.getName());
                    boolean algorithmHasMatchedChildren = false;
                    compartmentHasMatchedChildren |= algorithmMatches;

                    for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                        if (!Files.exists(outputSlot.getSlotStoragePath().resolve("data-table.json")))
                            continue;
                        DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(outputSlot);

                        if (algorithmMatches || searchTextField.test(outputSlot.getName())) {
                            algorithmNode.add(slotNode);
                            algorithmHasMatchedChildren = true;
                            compartmentHasMatchedChildren = true;
                        }
                    }
                    if (algorithmNode.getChildCount() > 0 && (algorithmMatches || algorithmHasMatchedChildren))
                        compartmentNode.add(algorithmNode);
                }
            }

            if (compartmentNode.getChildCount() > 0 && (compartmentMatches || compartmentHasMatchedChildren))
                root.add(compartmentNode);
        }
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        UIUtils.expandAllTree(tree);

        tree.getSelectionModel().setSelectionPath(new TreePath(root));

        treeScrollPane.getVerticalScrollBar().setValue(scrollPosition);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JTree();
        tree.setCellRenderer(new JIPipeResultTreeCellRenderer());
        treeScrollPane = new JScrollPane(tree);
        add(treeScrollPane, BorderLayout.CENTER);

        searchTextField = new SearchTextField();
        searchTextField.addActionListener(e -> refreshTree());
        add(searchTextField, BorderLayout.NORTH);
    }

    /**
     * @return The tree component
     */
    public JTree getTree() {
        return tree;
    }
}
