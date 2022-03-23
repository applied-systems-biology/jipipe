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
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.JIPipeProjectCacheState;
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
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
    private JIPipeCacheTree tree;

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
        tree = new JIPipeCacheTree(getProjectWorkbench());

        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, new JPanel(), AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        tree.getTree().addTreeSelectionListener(e -> {
            Object lastPathComponent = e.getPath().getLastPathComponent();
            if (lastPathComponent instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
                if (userObject instanceof JIPipeDataSlot) {
                    showDataSlot((JIPipeDataSlot) userObject);
                } else if (userObject instanceof JIPipeProjectCompartment) {
                    showDataSlotsOfCompartment((JIPipeProjectCompartment) userObject);
                } else if (userObject instanceof JIPipeGraphNode) {
                    showDataSlotsOfAlgorithm((JIPipeGraphNode) userObject);
                } else if (userObject instanceof JIPipeProjectCacheState) {
                    JIPipeGraphNode algorithm = (JIPipeGraphNode) ((DefaultMutableTreeNode) ((DefaultMutableTreeNode) lastPathComponent).getParent()).getUserObject();
                    showDataSlotsOfState(algorithm, (JIPipeProjectCacheState) userObject);
                } else {
                    showAllDataSlots();
                }
            }
        });

        initializeToolbar();
    }

    private void showDataSlotsOfState(JIPipeGraphNode algorithm, JIPipeProjectCacheState state) {
        List<JIPipeDataSlot> result = new ArrayList<>();
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(algorithm.getUUIDInParentGraph());
        if (stateMap != null) {
            Map<String, JIPipeDataSlot> slotMap = stateMap.getOrDefault(state, null);
            if (slotMap != null) {
                result.addAll(slotMap.values());
            }
        }
        showDataSlots(result);
    }

    private void showAllDataSlots() {
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeGraphNode node : getProject().getGraph().getGraphNodes()) {
            Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(node.getUUIDInParentGraph());
            if (stateMap != null) {
                for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                    result.addAll(stateEntry.getValue().values());
                }
            }
        }
        showDataSlots(result);
    }

    private void showDataSlotsOfAlgorithm(JIPipeGraphNode algorithm) {
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(algorithm.getUUIDInParentGraph());
        if (stateMap != null) {
            List<JIPipeDataSlot> result = new ArrayList<>();
            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                result.addAll(stateEntry.getValue().values());
            }
            showDataSlots(result);
        }
    }

    private void showDataSlotsOfCompartment(JIPipeProjectCompartment compartment) {
        List<JIPipeDataSlot> result = new ArrayList<>();
        UUID uuid = compartment.getUUIDInParentGraph();
        for (JIPipeGraphNode algorithm : getProject().getGraph().getGraphNodes()) {
            if (Objects.equals(algorithm.getCompartmentUUIDInParentGraph(), uuid)) {
                Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(algorithm.getUUIDInParentGraph());
                if (stateMap != null) {
                    for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                        result.addAll(stateEntry.getValue().values());
                    }
                }
            }
        }
        showDataSlots(result);
    }

    private void showDataSlots(List<JIPipeDataSlot> slots) {
        JIPipeExtendedMultiDataTableInfoUI ui = new JIPipeExtendedMultiDataTableInfoUI(getProjectWorkbench(), slots, true);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        JIPipeExtendedDataTableInfoUI ui = new JIPipeExtendedDataTableInfoUI(getProjectWorkbench(), dataSlot, true);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton clearOutdatedButton = new JButton("Clear outdated", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearOutdatedButton.addActionListener(e -> getProject().getCache().autoClean(false, true, new JIPipeProgressInfo()));
        toolBar.add(clearOutdatedButton);

        JButton clearAllButton = new JButton("Clear all", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearAllButton.addActionListener(e -> getProject().getCache().clear());
        toolBar.add(clearAllButton);

        add(toolBar, BorderLayout.NORTH);
    }

    public JIPipeCacheTree getTree() {
        return tree;
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        tree.refreshTree();
        showAllDataSlots();
    }
}
