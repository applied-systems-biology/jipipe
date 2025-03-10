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

package org.hkijena.jipipe.desktop.app.cache;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearAllRun;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearOutdatedRun;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.cache.cachetree.JIPipeDesktopCacheTreePanel;
import org.hkijena.jipipe.desktop.app.datatable.JIPipeDesktopExtendedDataTableUI;
import org.hkijena.jipipe.desktop.app.datatable.JIPipeDesktopExtendedMultiDataTableUI;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UI around an {@link JIPipeGraphRun} result
 */
public class JIPipeDesktopCacheBrowserUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeCache.ModifiedEventListener {
    private final StaticDebouncer refreshTreeAndSlotsDebouncer;
    private JSplitPane splitPane;
    private JIPipeDesktopCacheTreePanel tree;

    /**
     * @param workbenchUI the workbench
     */
    public JIPipeDesktopCacheBrowserUI(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI);
        this.refreshTreeAndSlotsDebouncer = new StaticDebouncer(1500, this::refreshTreeAndSlots);
        initialize();
        showAllDataSlots();

        getProject().getCache().getModifiedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JIPipeDesktopCacheTreePanel(getDesktopProjectWorkbench());

        splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, new JPanel(), JIPipeDesktopSplitPane.RATIO_1_TO_3);
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
            if (slotMap != null) {
                result.addAll(getSortedDataTables(slotMap, node));
            }
        }
        showDataTables(result);
    }

    private void showDataSlotsOfNode(JIPipeGraphNode node) {
        Map<String, JIPipeDataTable> slotMap = getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());
        List<JIPipeDataTable> result = new ArrayList<>();
        if (slotMap != null) {
            result.addAll(getSortedDataTables(slotMap, node));
        }
        showDataTables(result);
    }

    private void showDataSlotsOfCompartment(JIPipeProjectCompartment compartment) {
        List<JIPipeDataTable> result = new ArrayList<>();
        UUID uuid = compartment.getUUIDInParentGraph();
        for (JIPipeGraphNode algorithm : getProject().getGraph().getGraphNodes()) {
            if (Objects.equals(algorithm.getCompartmentUUIDInParentGraph(), uuid)) {
                Map<String, JIPipeDataTable> slotMap = getProject().getCache().query(algorithm, algorithm.getUUIDInParentGraph(), new JIPipeProgressInfo());
                if (slotMap != null) {
                    result.addAll(getSortedDataTables(slotMap, algorithm));
                }
            }
        }
        showDataTables(result);
    }

    private List<JIPipeDataTable> getSortedDataTables(Map<String, JIPipeDataTable> slotMap, JIPipeGraphNode graphNode) {
        List<JIPipeDataTable> result = new ArrayList<>();
        for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
            JIPipeDataTable dataTable = slotMap.getOrDefault(outputSlot.getName(), null);
            if (dataTable != null) {
                result.add(dataTable);
            }
        }
        return result;
    }

    private void showDataTables(List<JIPipeDataTable> dataTables) {
        JIPipeDesktopExtendedMultiDataTableUI ui = new JIPipeDesktopExtendedMultiDataTableUI(getDesktopProjectWorkbench(), dataTables.stream().map(WeakStore::new).collect(Collectors.toList()), true);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void showDataTable(JIPipeDataTable dataTable) {
        JIPipeDesktopExtendedDataTableUI ui = new JIPipeDesktopExtendedDataTableUI(getDesktopProjectWorkbench(), new WeakStore<>(dataTable), true, false);
        splitPane.setRightComponent(ui);
        revalidate();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton clearOutdatedButton = new JButton("Clear outdated", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearOutdatedButton.addActionListener(e -> JIPipeRunnableQueue.getInstance().enqueue(new JIPipeCacheClearOutdatedRun(getProject().getCache())));
        toolBar.add(clearOutdatedButton);

        JButton clearAllButton = new JButton("Clear all", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearAllButton.addActionListener(e -> JIPipeRunnableQueue.getInstance().enqueue(new JIPipeCacheClearAllRun(getProject().getCache())));
        toolBar.add(clearAllButton);

        add(toolBar, BorderLayout.NORTH);
    }

    public JIPipeDesktopCacheTreePanel getTree() {
        return tree;
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        refreshTreeAndSlotsDebouncer.debounce();
    }

    private void refreshTreeAndSlots() {
        tree.refreshTree();
        showAllDataSlots();
    }
}
