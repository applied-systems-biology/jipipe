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

package org.hkijena.jipipe.desktop.app.batchassistant;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.datatable.JIPipeDesktopExtendedDataTableUI;
import org.hkijena.jipipe.desktop.app.datatable.JIPipeDesktopExtendedMultiDataTableUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.data.OwningStore;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Browser for a Data batch, similar to the cache browser
 */
public class JIPipeDesktopDataBatchBrowserUI extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeMultiIterationStep iterationStep;
    private JSplitPane splitPane;
    private JIPipeDesktopDataBatchTree tree;

    /**
     * @param workbenchUI   the workbench
     * @param iterationStep the data batch
     */
    public JIPipeDesktopDataBatchBrowserUI(JIPipeDesktopWorkbench workbenchUI, JIPipeMultiIterationStep iterationStep) {
        super(workbenchUI);
        this.iterationStep = iterationStep;
        initialize();
        showCurrentlySelectedNode();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JIPipeDesktopDataBatchTree(getDesktopWorkbench(), iterationStep);

        splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, tree,
                new JPanel(), AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        tree.getTree().addTreeSelectionListener(e -> showCurrentlySelectedNode());
    }

    private void showCurrentlySelectedNode() {
        Object lastPathComponent = tree.getTree().getLastSelectedPathComponent();
        if (lastPathComponent instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
            if (userObject instanceof JIPipeDataSlot) {
                showDataSlot((JIPipeDataSlot) userObject);
            } else {
                showAllDataSlots();
            }
        }
    }

    private void showAllDataSlots() {
        showDataSlots(new ArrayList<>(iterationStep.getInputSlotRows().keySet()));
    }

    private void showDataSlots(List<JIPipeDataSlot> slots) {
        List<JIPipeDataTable> filtered = new ArrayList<>();
        for (JIPipeDataSlot slot : slots) {
            filtered.add(slot.slice(iterationStep.getInputRows(slot)));
        }
        JIPipeDesktopExtendedMultiDataTableUI ui = new JIPipeDesktopExtendedMultiDataTableUI(getDesktopWorkbench(), filtered.stream().map(OwningStore::new).collect(Collectors.toList()), false);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        JIPipeDataTable filtered = dataSlot.slice(iterationStep.getInputSlotRows().get(dataSlot));
        JIPipeDesktopExtendedDataTableUI ui = new JIPipeDesktopExtendedDataTableUI(getDesktopWorkbench(), new OwningStore<>(filtered), true);
        splitPane.setRightComponent(ui);
        revalidate();
    }
}
