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
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearAllRun;
import org.hkijena.jipipe.api.cache.JIPipeCacheClearOutdatedRun;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI that monitors the queue
 */
public class JIPipeCacheManagerUI extends JButton implements JIPipeProjectWorkbenchAccess {

    private final JIPipeWorkbench workbench;
    private final JPopupMenu menu = new JPopupMenu();

    /**
     * Creates new instance
     *
     * @param workbenchUI the workbench
     */
    public JIPipeCacheManagerUI(JIPipeProjectWorkbench workbenchUI) {
        this.workbench = workbenchUI;
        initialize();
        updateStatus();

        getProject().getCache().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setIcon(UIUtils.getIconFromResources("actions/database.png"));
        UIUtils.addReloadablePopupMenuToComponent(this, menu, this::reloadMenu);
    }

    private void reloadMenu() {
        menu.removeAll();

        JMenuItem openCacheBrowser = new JMenuItem("Open cache browser", UIUtils.getIconFromResources("actions/zoom.png"));
        openCacheBrowser.addActionListener(e -> getProjectWorkbench().openCacheBrowser());
        menu.add(openCacheBrowser);

        menu.addSeparator();

        JMenuItem readCache = new JMenuItem("Restore cache from ZIP/directory", UIUtils.getIconFromResources("actions/document-import.png"));
        readCache.addActionListener(e -> getProjectWorkbench().restoreCacheFromZIPOrDirectory());
        menu.add(readCache);

        JMenuItem writeCacheToDirectory = new JMenuItem("Export cache to ZIP/directory", UIUtils.getIconFromResources("actions/document-export.png"));
        writeCacheToDirectory.addActionListener(e -> getProjectWorkbench().saveProjectAndCache("Export cache", false));
        menu.add(writeCacheToDirectory);

        if (!getProject().getCache().isEmpty()) {
            menu.addSeparator();

            JMenuItem clearOutdated = new JMenuItem("Clear outdated", UIUtils.getIconFromResources("actions/clock.png"));
            clearOutdated.setToolTipText("Removes all cached items that are have no representation in the project graph, anymore. " +
                    "This includes items where the algorithm parameters have been changed.");
            clearOutdated.addActionListener(e -> JIPipeRunnerQueue.getInstance().enqueue(new JIPipeCacheClearOutdatedRun(getProject().getCache())));
            menu.add(clearOutdated);

            JMenuItem clearAll = new JMenuItem("Clear all", UIUtils.getIconFromResources("actions/delete.png"));
            clearAll.setToolTipText("Removes all cached items.");
            clearAll.addActionListener(e -> JIPipeRunnerQueue.getInstance().enqueue(new JIPipeCacheClearAllRun(getProject().getCache())));
            menu.add(clearAll);
        }
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
    @Subscribe
    public void onCacheUpdated(JIPipeCache.ModifiedEvent event) {
        updateStatus();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
