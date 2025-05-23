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

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearAllRun;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearOutdatedRun;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import java.awt.*;

/**
 * UI that monitors the queue
 */
public class JIPipeDesktopCacheManagerUI extends JButton implements JIPipeDesktopProjectWorkbenchAccess, JIPipeCache.ModifiedEventListener {

    private final JIPipeDesktopWorkbench desktopWorkbench;
    private final JPopupMenu menu = new JPopupMenu();
    private final StaticDebouncer updateStatusDebouncer;

    public JIPipeDesktopCacheManagerUI(JIPipeDesktopProjectWorkbench workbenchUI) {
        this.desktopWorkbench = workbenchUI;
        this.updateStatusDebouncer = new StaticDebouncer(500, this::updateStatus);
        initialize();
        updateStatus();

        getProject().getCache().getModifiedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setIcon(UIUtils.getIconFromResources("actions/database.png"));
        UIUtils.addReloadablePopupMenuToButton(this, menu, this::reloadMenu);
    }

    private void reloadMenu() {
        menu.removeAll();

        JMenuItem openCacheBrowser = new JMenuItem("Open cache browser", UIUtils.getIconFromResources("actions/zoom.png"));
        openCacheBrowser.addActionListener(e -> getDesktopProjectWorkbench().openCacheBrowser());
        menu.add(openCacheBrowser);

        menu.addSeparator();

        JMenuItem readCache = new JMenuItem("Restore cache from ZIP/directory", UIUtils.getIconFromResources("actions/document-import.png"));
        readCache.addActionListener(e -> getDesktopProjectWorkbench().restoreCacheFromZIPOrDirectory());
        menu.add(readCache);

        JMenuItem writeCacheToDirectory = new JMenuItem("Export cache to ZIP/directory", UIUtils.getIconFromResources("actions/document-export.png"));
        writeCacheToDirectory.addActionListener(e -> getDesktopProjectWorkbench().saveProjectAndCache("Export cache", false));
        menu.add(writeCacheToDirectory);

        if (!getProject().getCache().isEmpty()) {
            menu.addSeparator();

            JMenuItem clearOutdated = new JMenuItem("Clear outdated", UIUtils.getIconFromResources("actions/clock.png"));
            clearOutdated.setToolTipText("Removes all cached items that are have no representation in the project graph, anymore. " +
                    "This includes items where the algorithm parameters have been changed.");
            clearOutdated.addActionListener(e -> JIPipeRunnableQueue.getInstance().enqueue(new JIPipeCacheClearOutdatedRun(getProject().getCache())));
            menu.add(clearOutdated);

            JMenuItem clearAll = new JMenuItem("Clear all", UIUtils.getIconFromResources("actions/delete.png"));
            clearAll.setToolTipText("Removes all cached items.");
            clearAll.addActionListener(e -> JIPipeRunnableQueue.getInstance().enqueue(new JIPipeCacheClearAllRun(getProject().getCache())));
            menu.add(clearAll);
        }

        menu.addSeparator();

        JCheckBoxMenuItem cacheIgnoreNodeStates = new JCheckBoxMenuItem("Ignore parameter changes");
        cacheIgnoreNodeStates.setToolTipText("If enabled, the caching system will not delete cached items if node parameters have changed.\nUse this button if caches keep being accidentally cleared.");
        cacheIgnoreNodeStates.setState(getDesktopProjectWorkbench().getProject().getCache().isIgnoreNodeFunctionalEquals());
        cacheIgnoreNodeStates.addActionListener(e -> getDesktopProjectWorkbench().getProject().getCache().setIgnoreNodeFunctionalEquals(cacheIgnoreNodeStates.getState()));
        menu.add(cacheIgnoreNodeStates);
    }

    /**
     * Updates the UI status
     */
    public void updateStatus() {
        if (getProject().getCache().isEmpty()) {
            setText("Cache (Empty)");
        } else {
            int size = getProject().getCache().size();
            setText(size == 1 ? "Cache (1 item)" : "Cache (" + size + " items)");
        }
    }

    /**
     * Triggered when the cache was updated
     *
     * @param event generated event
     */
    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {
        updateStatusDebouncer.debounce();
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return getDesktopWorkbench();
    }
}
