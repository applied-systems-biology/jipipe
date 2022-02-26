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
import org.hkijena.jipipe.api.JIPipeProjectCacheQuery;
import org.hkijena.jipipe.api.JIPipeProjectCacheState;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchAccess;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ZoomFlatIconButton;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the cache for a specific data slot.
 */
public class JIPipeDataSlotCacheManagerUI extends JIPipeProjectWorkbenchPanel {

    private final JIPipeDataSlot dataSlot;
    private final JIPipeGraphCanvasUI graphUI;
    private JButton cacheButton;
    private JPopupMenu contextMenu;

    /**
     * @param workbenchUI The workbench UI
     * @param dataSlot    the data slot
     * @param graphUI     the canvas
     */
    public JIPipeDataSlotCacheManagerUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot dataSlot, JIPipeGraphCanvasUI graphUI) {
        super(workbenchUI);
        this.dataSlot = dataSlot;
        this.graphUI = graphUI;
        initialize();
        updateStatus();

        getProject().getCache().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        contextMenu = new JPopupMenu();

        cacheButton = new ZoomFlatIconButton(UIUtils.getIconFromResources("actions/database.png"), graphUI);
        cacheButton.setToolTipText("This slot has cached data");
        UIUtils.addReloadablePopupMenuToComponent(cacheButton, contextMenu, this::reloadContextMenu);
        add(cacheButton);
    }

    private void reloadContextMenu() {
        contextMenu.removeAll();
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(getProject());
        JIPipeProjectCacheState currentState = query.getCachedId(getDataSlot().getNode());

        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(getDataSlot().getNode());
        if (stateMap != null) {
            JMenuItem openCurrent = createOpenStateButton(stateMap, currentState, "Open current snapshot");
            if (openCurrent != null) {
                contextMenu.add(openCurrent);
            }
            JMenu previousMenu = new JMenu("All snapshots");
            previousMenu.setIcon(UIUtils.getIconFromResources("actions/clock.png"));
            for (JIPipeProjectCacheState state : stateMap.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                JMenuItem item = createOpenStateButton(stateMap, state, "Open snapshot from " + state.getGenerationTime().format(DateTimeFormatter.ISO_LOCAL_DATE) + " " +
                        state.getGenerationTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                if (item != null) {
                    previousMenu.add(item);
                }
            }
            if (previousMenu.getItemCount() > 0) {
                contextMenu.add(previousMenu);
                contextMenu.addSeparator();
            }
        }

        JMenuItem clearOutdated = new JMenuItem("Clear outdated", UIUtils.getIconFromResources("actions/clock.png"));
        clearOutdated.setToolTipText("Removes all cached items that are have no representation in the project graph, anymore. " +
                "This includes items where the algorithm parameters have been changed.");
        clearOutdated.addActionListener(e -> getProject().getCache().autoClean(false, true, new JIPipeProgressInfo()));
        contextMenu.add(clearOutdated);

        JMenuItem clearAll = new JMenuItem("Clear all", UIUtils.getIconFromResources("actions/delete.png"));
        clearAll.setToolTipText("Removes all cached items for this node.");
        clearAll.addActionListener(e -> getProject().getCache().clear((JIPipeAlgorithm) dataSlot.getNode()));
        contextMenu.add(clearAll);
    }

    private JMenuItem createOpenStateButton(Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap, JIPipeProjectCacheState state, String label) {
        Map<String, JIPipeDataSlot> slotMap = stateMap.getOrDefault(state, null);
        if (slotMap == null)
            return null;
        JIPipeDataSlot cachedSlot = slotMap.getOrDefault(getDataSlot().getName(), null);
        if (cachedSlot == null)
            return null;

        JMenuItem item = new JMenuItem(label);
        item.setIcon(UIUtils.getIconFromResources("actions/camera.png"));
        item.setToolTipText("Opens the currently cached data as table");
        item.addActionListener(e -> openData(state));
        return item;
    }

    private void openData(JIPipeProjectCacheState state) {
//        JIPipeExtendedDataTableInfoUI cacheTable = new JIPipeExtendedDataTableInfoUI(getProjectWorkbench(), cachedSlot);
//        String tabName = getDataSlot().getAlgorithm().getName() + "/" + getDataSlot().getName() + " @ " + state.getGenerationTime().format(DateTimeFormatter.ISO_LOCAL_DATE) + " " +
//                state.getGenerationTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        JIPipeCacheBrowserUI cacheTable = new JIPipeCacheBrowserUI(getProjectWorkbench());
        cacheTable.getTree().selectDataSlot(state, getDataSlot());
        getWorkbench().getDocumentTabPane().addTab("Cache browser",
                UIUtils.getIconFromResources("actions/database.png"),
                cacheTable,
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    public JIPipeDataSlot getDataSlot() {
        return dataSlot;
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onCacheUpdated(JIPipeProjectCache.ModifiedEvent event) {
        if (!isDisplayable())
            return;
        updateStatus();
    }

    private void updateStatus() {
        JIPipeProjectCache cache = getProject().getCache();
        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = cache.extract(getDataSlot().getNode());
        int dataRows = 0;
        if (stateMap != null) {
            for (Map<String, JIPipeDataSlot> slotMap : stateMap.values()) {
                JIPipeDataSlot equivalentSlot = slotMap.getOrDefault(getDataSlot().getName(), null);
                if (equivalentSlot != null) {
                    dataRows += equivalentSlot.getRowCount();
                }
            }
        }

        cacheButton.setVisible(dataRows > 0);
    }
}
