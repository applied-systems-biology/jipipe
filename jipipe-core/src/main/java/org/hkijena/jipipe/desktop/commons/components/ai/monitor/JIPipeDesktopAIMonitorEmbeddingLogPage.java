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

package org.hkijena.jipipe.desktop.commons.components.ai.monitor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopProgressTreePanel;

import java.awt.*;

/**
 * Log page for the AI monitor window that displays the embedding model operation log.
 * <p>
 * Uses {@link JIPipeDesktopProgressTreePanel} connected to
 * {@link JIPipeAIServiceComponent#getEmbeddingProgressInfo()}: the page subscribes to the
 * handle's store batch events via {@link JIPipeProgressInfo#subscribeToBatchEvents(JIPipeProgressLog.ProgressBatchEventListener)}
 * (the subscription follows the handle across store adoption) and forwards the batches to the
 * tree panel, which renders the single-root record stream.
 * <p>
 * Follows the same design pattern as {@code JIPipeDesktopCacheMonitorDataCacheLogPage}.
 */
public class JIPipeDesktopAIMonitorEmbeddingLogPage extends JIPipeDesktopAIMonitorPage {

    private JIPipeDesktopProgressTreePanel treePanel;
    private JIPipeProgressInfo.BatchEventSubscription batchSubscription;

    public JIPipeDesktopAIMonitorEmbeddingLogPage(JIPipeDesktopAIMonitorWindow window) {
        super(window);
        initialize();
    }

    private void initialize() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();

        treePanel = new JIPipeDesktopProgressTreePanel(getDesktopWorkbench());
        // The Show log checkbox is a run-manager convenience; the monitor page always shows the tree
        treePanel.setShowLogSelected(true);
        add(treePanel, BorderLayout.CENTER);

        bindStore(aiService.getEmbeddingProgressInfo());
    }

    /**
     * Binds the embedding progress handle's store to the tree panel. The batch listener is
     * subscribed before the store is set - the safe ordering for any store that could still emit.
     * The handle is file-backed (batches flow); a silent store would render statically from the
     * tree panel's catch-up scan, and a null store leaves the panel empty.
     */
    private void bindStore(JIPipeProgressInfo progressInfo) {
        if (progressInfo == null || progressInfo.getStore() == null) {
            return;
        }
        batchSubscription = progressInfo.subscribeToBatchEvents(this::onEmbeddingBatch);
        treePanel.setProgressStore(progressInfo.getStore());
    }

    private void onEmbeddingBatch(JIPipeProgressLog.ProgressBatchEvent event) {
        treePanel.onProgressBatch(event);
    }

    @Override
    public void refresh() {
        // The tree panel auto-updates via the store batch subscription
    }

    /**
     * Releases the store batch subscription and stops the tree panel's timer and background
     * reads. Called by the monitor window's dispose chain.
     */
    @Override
    public void dispose() {
        if (batchSubscription != null) {
            batchSubscription.close();
            batchSubscription = null;
        }
        treePanel.dispose();
        super.dispose();
    }
}
