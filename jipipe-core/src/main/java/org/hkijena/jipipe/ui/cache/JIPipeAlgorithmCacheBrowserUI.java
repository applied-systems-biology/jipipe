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
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * UI around an {@link JIPipeRun} result
 */
public class JIPipeAlgorithmCacheBrowserUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeGraphNode graphNode;
    private JSplitPane splitPane;
    private JIPipeAlgorithmCacheTree tree;

    /**
     * @param workbenchUI the workbench
     * @param graphNode   the node
     */
    public JIPipeAlgorithmCacheBrowserUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode graphNode) {
        super(workbenchUI);
        this.graphNode = graphNode;
        initialize();
        showAllDataSlots();

        getProject().getCache().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JIPipeAlgorithmCacheTree(getProjectWorkbench(), graphNode);

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

        tree.getTree().addTreeSelectionListener(e -> {
            Object lastPathComponent = e.getPath().getLastPathComponent();
            if (lastPathComponent instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
                if (userObject instanceof JIPipeDataSlot) {
                    showDataSlot((JIPipeDataSlot) userObject);
                } else if (userObject instanceof JIPipeGraphNode) {
                    showDataSlotsOfAlgorithm((JIPipeGraphNode) userObject);
                } else if (userObject instanceof JIPipeProjectCache.State) {
                    showDataSlotsOfState((JIPipeProjectCache.State) userObject);
                } else {
                    showAllDataSlots();
                }
            }
        });

        initializeToolbar();
    }

    private void showDataSlotsOfState(JIPipeProjectCache.State state) {
        List<JIPipeDataSlot> result = new ArrayList<>();
        Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract((JIPipeAlgorithm) graphNode);
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
        Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract((JIPipeAlgorithm) graphNode);
        if (stateMap != null) {
            for (Map.Entry<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                result.addAll(stateEntry.getValue().values());
            }
        }
        showDataSlots(result);
    }

    private void showDataSlotsOfAlgorithm(JIPipeGraphNode algorithm) {
        Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract((JIPipeAlgorithm) algorithm);
        if (stateMap != null) {
            List<JIPipeDataSlot> result = new ArrayList<>();
            for (Map.Entry<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                result.addAll(stateEntry.getValue().values());
            }
            showDataSlots(result);
        }
    }

    private void showDataSlots(List<JIPipeDataSlot> slots) {
        JIPipeCacheMultiDataSlotTableUI ui = new JIPipeCacheMultiDataSlotTableUI(getProjectWorkbench(), slots);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataSlot(JIPipeDataSlot dataSlot) {
        JIPipeCacheDataSlotTableUI ui = new JIPipeCacheDataSlotTableUI(getProjectWorkbench(), dataSlot);
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

    public JIPipeAlgorithmCacheTree getTree() {
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
