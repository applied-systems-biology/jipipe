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
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataSlotTableUI;
import org.hkijena.jipipe.ui.cache.JIPipeCacheMultiDataSlotTableUI;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Browser for a Data batch, similar to the cache browser
 */
public class DataBatchBrowserUI extends JIPipeWorkbenchPanel {
    private final JIPipeMergingDataBatch dataBatch;
    private JSplitPane splitPane;
    private DataBatchTree tree;

    /**
     * @param workbenchUI the workbench
     * @param dataBatch   the data batch
     */
    public DataBatchBrowserUI(JIPipeWorkbench workbenchUI, JIPipeMergingDataBatch dataBatch) {
        super(workbenchUI);
        this.dataBatch = dataBatch;
        initialize();
        showCurrentlySelectedNode();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new DataBatchTree(getWorkbench(), dataBatch);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tree,
                new JPanel());
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
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
        List<JIPipeDataSlot> filtered = new ArrayList<>();
        for (JIPipeDataSlot slot : slots) {
            filtered.add(slot.slice(dataBatch.getInputRows(slot)));
        }
        JIPipeCacheMultiDataSlotTableUI ui = new JIPipeCacheMultiDataSlotTableUI(getWorkbench(), filtered, false);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        JIPipeDataSlot filtered = dataSlot.slice(dataBatch.getInputSlotRows().get(dataSlot));
        JIPipeCacheDataSlotTableUI ui = new JIPipeCacheDataSlotTableUI(getWorkbench(), filtered);
        splitPane.setRightComponent(ui);
        revalidate();
    }
}
