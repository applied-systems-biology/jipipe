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
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopRunQueueLoggerPanel;

import java.awt.*;

/**
 * Log page for the AI monitor window that displays the AI service task queue log.
 * <p>
 * Uses {@link JIPipeDesktopRunQueueLoggerPanel} connected to
 * {@link JIPipeAIServiceComponent#getQueue()}.
 * <p>
 * Follows the same design pattern as {@code JIPipeDesktopCacheMonitorDataCacheLogPage}.
 */
public class JIPipeDesktopAIMonitorLogPage extends JIPipeDesktopAIMonitorPage {

    private JIPipeDesktopRunQueueLoggerPanel loggerPanel;

    public JIPipeDesktopAIMonitorLogPage(JIPipeDesktopAIMonitorWindow window) {
        super(window);
        initialize();
    }

    private void initialize() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        JIPipeRunnableQueue queue = aiService.getQueue();

        loggerPanel = new JIPipeDesktopRunQueueLoggerPanel(getDesktopWorkbench(), queue);
        loggerPanel.addDefaultCancelButton();

        add(loggerPanel, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        // The log panel auto-updates via its event subscriptions
    }
}
