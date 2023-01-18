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
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearOutdatedRun;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ZoomFlatIconButton;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Map;

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

        JMenuItem openInNewWindow = new JMenuItem("Open in new window (this node)", UIUtils.getIconFromResources("actions/link.png"));
        openInNewWindow.setToolTipText("Opens the cache in a new window.");
        openInNewWindow.addActionListener(e -> openCacheInNewWindow());
        contextMenu.add(openInNewWindow);

        JMenuItem openInNewTab = new JMenuItem("Open in new tab (this node)", UIUtils.getIconFromResources("actions/tab_new.png"));
        openInNewTab.setToolTipText("Opens the cache in a new tab.");
        openInNewTab.addActionListener(e -> openCacheInNewTab());
        contextMenu.add(openInNewTab);

        contextMenu.addSeparator();

        JMenuItem clearAll = new JMenuItem("Clear all (this node)", UIUtils.getIconFromResources("actions/delete.png"));
        clearAll.setToolTipText("Removes all cached items for this node.");
        clearAll.addActionListener(e -> getProject().getCache().clearAll(dataSlot.getNode().getUUIDInParentGraph(), new JIPipeProgressInfo()));
        contextMenu.add(clearAll);

        JMenuItem clearOutdated = new JMenuItem("Clear outdated (this node)", UIUtils.getIconFromResources("actions/clear-brush.png"));
        clearOutdated.setToolTipText("Removes all cached items that are have no representation in the project graph, anymore. " +
                "This includes items where the algorithm parameters have been changed.");
        clearOutdated.addActionListener(e -> JIPipeRunnerQueue.getInstance().enqueue(new JIPipeCacheClearOutdatedRun(getProject().getCache())));
        contextMenu.add(clearOutdated);
    }

    private void openCacheInNewTab() {
        JIPipeCacheBrowserUI cacheTable = new JIPipeCacheBrowserUI(getProjectWorkbench());
        cacheTable.getTree().selectDataSlot(getDataSlot());
        getWorkbench().getDocumentTabPane().addTab("Cache browser",
                UIUtils.getIconFromResources("actions/database.png"),
                cacheTable,
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void openCacheInNewWindow() {
        JIPipeAlgorithmCacheBrowserUI browserUI = new JIPipeAlgorithmCacheBrowserUI((JIPipeProjectWorkbench) getWorkbench(), getDataSlot().getNode(), graphUI);
        JFrame frame = new JFrame("Cache browser: " + getDataSlot().getNode().getName());
        frame.setAlwaysOnTop(GeneralUISettings.getInstance().isOpenUtilityWindowsAlwaysOnTop());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(browserUI);
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.pack();
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
    public void onCacheUpdated(JIPipeCache.ModifiedEvent event) {
        if (!isDisplayable())
            return;
        updateStatus();
    }

    private void updateStatus() {
        JIPipeCache cache = getProject().getCache();
        Map<String, JIPipeDataTable> slotMap = cache.query(getDataSlot().getNode(), getDataSlot().getNode().getUUIDInParentGraph(), new JIPipeProgressInfo());
        int dataRows = 0;
        if (slotMap != null) {
            JIPipeDataTable equivalentSlot = slotMap.getOrDefault(getDataSlot().getName(), null);
            if (equivalentSlot != null) {
                dataRows += equivalentSlot.getRowCount();
            }
        }
        cacheButton.setVisible(dataRows > 0);
    }
}
