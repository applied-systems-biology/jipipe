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

package org.hkijena.jipipe.ui.batchassistant;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMultiDataBatch;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.datatable.JIPipeExtendedDataTableUI;
import org.hkijena.jipipe.ui.datatable.JIPipeExtendedMultiDataTableUI;
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
public class DataBatchBrowserUI extends JIPipeWorkbenchPanel {
    private final JIPipeMultiDataBatch dataBatch;
    private JSplitPane splitPane;
    private DataBatchTree tree;

    /**
     * @param workbenchUI the workbench
     * @param dataBatch   the data batch
     */
    public DataBatchBrowserUI(JIPipeWorkbench workbenchUI, JIPipeMultiDataBatch dataBatch) {
        super(workbenchUI);
        this.dataBatch = dataBatch;
        initialize();
        showCurrentlySelectedNode();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new DataBatchTree(getWorkbench(), dataBatch);

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
        showDataSlots(new ArrayList<>(dataBatch.getInputSlotRows().keySet()));
    }

    private void showDataSlots(List<JIPipeDataSlot> slots) {
        List<JIPipeDataTable> filtered = new ArrayList<>();
        for (JIPipeDataSlot slot : slots) {
            filtered.add(slot.slice(dataBatch.getInputRows(slot)));
        }
        JIPipeExtendedMultiDataTableUI ui = new JIPipeExtendedMultiDataTableUI(getWorkbench(), filtered.stream().map(OwningStore::new).collect(Collectors.toList()), false);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        JIPipeDataTable filtered = dataSlot.slice(dataBatch.getInputSlotRows().get(dataSlot));
        JIPipeExtendedDataTableUI ui = new JIPipeExtendedDataTableUI(getWorkbench(), new OwningStore<>(filtered), true);
        splitPane.setRightComponent(ui);
        revalidate();
    }
}
