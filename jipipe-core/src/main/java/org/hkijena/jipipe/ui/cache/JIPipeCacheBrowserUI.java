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

package org.hkijena.jipipe.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearAllRun;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearOutdatedRun;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.cachetree.JIPipeCacheTreePanel;
import org.hkijena.jipipe.ui.datatable.JIPipeExtendedDataTableUI;
import org.hkijena.jipipe.ui.datatable.JIPipeExtendedMultiDataTableUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * UI around an {@link JIPipeProjectRun} result
 */
public class JIPipeCacheBrowserUI extends JIPipeProjectWorkbenchPanel {
    private JSplitPane splitPane;
    private JIPipeCacheTreePanel tree;

    /**
     * @param workbenchUI the workbench
     */
    public JIPipeCacheBrowserUI(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        showAllDataSlots();

        getProject().getCache().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JIPipeCacheTreePanel(getProjectWorkbench());

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, new JPanel(), AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        tree.getTree().addTreeSelectionListener(e -> {
            Object lastPathComponent = e.getPath().getLastPathComponent();
            if (lastPathComponent instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
                if (userObject instanceof JIPipeDataSlot) {
                    showDataTable((JIPipeDataSlot) userObject);
                } else if (userObject instanceof JIPipeProjectCompartment) {
                    showDataSlotsOfCompartment((JIPipeProjectCompartment) userObject);
                } else if (userObject instanceof JIPipeGraphNode) {
                    showDataSlotsOfNode((JIPipeGraphNode) userObject);
                } else {
                    showAllDataSlots();
                }
            }
        });

        initializeToolbar();
    }

    private void showAllDataSlots() {
        List<JIPipeDataTable> result = new ArrayList<>();
        for (JIPipeGraphNode node : getProject().getGraph().getGraphNodes()) {
            Map<String, JIPipeDataTable> slotMap = getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
            if(slotMap != null) {
                result.addAll(slotMap.values());
            }
        }
        showDataTables(result);
    }

    private void showDataSlotsOfNode(JIPipeGraphNode node) {
        Map<String, JIPipeDataTable> slotMap = getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
        List<JIPipeDataTable> result = new ArrayList<>();
        if(slotMap != null) {
           result.addAll(slotMap.values());
        }
        showDataTables(result);
    }

    private void showDataSlotsOfCompartment(JIPipeProjectCompartment compartment) {
        List<JIPipeDataTable> result = new ArrayList<>();
        UUID uuid = compartment.getUUIDInParentGraph();
        for (JIPipeGraphNode algorithm : getProject().getGraph().getGraphNodes()) {
            if (Objects.equals(algorithm.getCompartmentUUIDInParentGraph(), uuid)) {
                Map<String, JIPipeDataTable> slotMap = getProject().getCache().query(algorithm, algorithm.getUUIDInParentGraph(), new JIPipeProgressInfo());
                if(slotMap != null) {
                    result.addAll(slotMap.values());
                }
            }
        }
        showDataTables(result);
    }

    private void showDataTables(List<JIPipeDataTable> dataTables) {
        JIPipeExtendedMultiDataTableUI ui = new JIPipeExtendedMultiDataTableUI(getProjectWorkbench(), dataTables, true);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataTable(JIPipeDataTable dataTable) {
        JIPipeExtendedDataTableUI ui = new JIPipeExtendedDataTableUI(getProjectWorkbench(), dataTable, true);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton clearOutdatedButton = new JButton("Clear outdated", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearOutdatedButton.addActionListener(e -> JIPipeRunnerQueue.getInstance().enqueue(new JIPipeCacheClearOutdatedRun(getProject().getCache())));
        toolBar.add(clearOutdatedButton);

        JButton clearAllButton = new JButton("Clear all", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearAllButton.addActionListener(e -> JIPipeRunnerQueue.getInstance().enqueue(new JIPipeCacheClearAllRun(getProject().getCache())));
        toolBar.add(clearAllButton);

        add(toolBar, BorderLayout.NORTH);
    }

    public JIPipeCacheTreePanel getTree() {
        return tree;
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(JIPipeCache.ModifiedEvent event) {
        tree.refreshTree();
        showAllDataSlots();
    }
}
