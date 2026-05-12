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
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopProgressLoggerPanel;

import java.awt.*;

/**
 * Log page for the AI monitor window that displays the embedding model operation log.
 * <p>
 * Uses {@link JIPipeDesktopProgressLoggerPanel} connected to
 * {@link JIPipeAIServiceComponent#getEmbeddingProgressInfo()}.
 * <p>
 * Follows the same design pattern as {@code JIPipeDesktopCacheMonitorDataCacheLogPage}.
 */
public class JIPipeDesktopAIMonitorEmbeddingLogPage extends JIPipeDesktopAIMonitorPage {

    private JIPipeDesktopProgressLoggerPanel loggerPanel;

    public JIPipeDesktopAIMonitorEmbeddingLogPage(JIPipeDesktopAIMonitorWindow window) {
        super(window);
        initialize();
    }

    private void initialize() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();

        loggerPanel = new JIPipeDesktopProgressLoggerPanel(getDesktopWorkbench(), "Embedding operations", true);
        loggerPanel.addProgressSource("Embedding", aiService.getEmbeddingProgressInfo(), true);

        add(loggerPanel, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        // The log panel auto-updates via its event subscriptions
    }
}
