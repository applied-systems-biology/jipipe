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

package org.hkijena.acaq5.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * UI around an {@link ACAQRun} result
 */
public class ACAQCacheBrowserUI extends ACAQProjectWorkbenchPanel {
    private JSplitPane splitPane;
    private ACAQCacheTree tree;

    /**
     * @param workbenchUI the workbench
     */
    public ACAQCacheBrowserUI(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        showAllDataSlots();

        getProject().getCache().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new ACAQCacheTree(getProjectWorkbench());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree,
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

        tree.getTree().addTreeSelectionListener(e -> {
            Object lastPathComponent = e.getPath().getLastPathComponent();
            if (lastPathComponent instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
                if (userObject instanceof ACAQDataSlot) {
                    showDataSlot((ACAQDataSlot) userObject);
                } else if (userObject instanceof ACAQProjectCompartment) {
                    showDataSlotsOfCompartment((ACAQProjectCompartment) userObject);
                } else if (userObject instanceof ACAQGraphNode) {
                    showDataSlotsOfAlgorithm((ACAQGraphNode) userObject);
                } else if (userObject instanceof ACAQProjectCache.State) {
                    ACAQGraphNode algorithm = (ACAQGraphNode) ((DefaultMutableTreeNode) ((DefaultMutableTreeNode) lastPathComponent).getParent()).getUserObject();
                    showDataSlotsOfState(algorithm, (ACAQProjectCache.State) userObject);
                } else {
                    showAllDataSlots();
                }
            }
        });

        initializeToolbar();
    }

    private void showDataSlotsOfState(ACAQGraphNode algorithm, ACAQProjectCache.State state) {
        List<ACAQDataSlot> result = new ArrayList<>();
        Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap = getProject().getCache().extract((ACAQAlgorithm) algorithm);
        if (stateMap != null) {
            Map<String, ACAQDataSlot> slotMap = stateMap.getOrDefault(state, null);
            if (slotMap != null) {
                result.addAll(slotMap.values());
            }
        }
        showDataSlots(result);
    }

    private void showAllDataSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQGraphNode node : getProject().getGraph().getAlgorithmNodes().values()) {
            Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap = getProject().getCache().extract((ACAQAlgorithm) node);
            if (stateMap != null) {
                for (Map.Entry<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateEntry : stateMap.entrySet()) {
                    result.addAll(stateEntry.getValue().values());
                }
            }
        }
        showDataSlots(result);
    }

    private void showDataSlotsOfAlgorithm(ACAQGraphNode algorithm) {
        Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap = getProject().getCache().extract((ACAQAlgorithm) algorithm);
        if (stateMap != null) {
            List<ACAQDataSlot> result = new ArrayList<>();
            for (Map.Entry<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateEntry : stateMap.entrySet()) {
                result.addAll(stateEntry.getValue().values());
            }
            showDataSlots(result);
        }
    }

    private void showDataSlotsOfCompartment(ACAQProjectCompartment compartment) {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQGraphNode algorithm : getProject().getGraph().getAlgorithmNodes().values()) {
            if (algorithm.getCompartment().equals(compartment.getName()) || algorithm.getCompartment().equals(compartment.getProjectCompartmentId())) {
                Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap = getProject().getCache().extract((ACAQAlgorithm) algorithm);
                if (stateMap != null) {
                    for (Map.Entry<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateEntry : stateMap.entrySet()) {
                        result.addAll(stateEntry.getValue().values());
                    }
                }
            }
        }
        showDataSlots(result);
    }

    private void showDataSlots(List<ACAQDataSlot> slots) {
        ACAQCacheMultiDataSlotTableUI ui = new ACAQCacheMultiDataSlotTableUI(getProjectWorkbench(), slots);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(ACAQDataSlot dataSlot) {
        ACAQCacheDataSlotTableUI ui = new ACAQCacheDataSlotTableUI(getProjectWorkbench(), dataSlot);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton clearOutdatedButton = new JButton("Clear outdated", UIUtils.getIconFromResources("clear-brush.png"));
        clearOutdatedButton.addActionListener(e -> getProject().getCache().autoClean(false, true));
        toolBar.add(clearOutdatedButton);

        JButton clearAllButton = new JButton("Clear all", UIUtils.getIconFromResources("clear-brush.png"));
        clearAllButton.addActionListener(e -> getProject().getCache().clear());
        toolBar.add(clearAllButton);

        add(toolBar, BorderLayout.NORTH);
    }

    public ACAQCacheTree getTree() {
        return tree;
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(ACAQProjectCache.ModifiedEvent event) {
        tree.refreshTree();
        showAllDataSlots();
    }
}
