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
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEvent;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeListener;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Monitor window for the AI service, following a page-based design pattern
 * inspired by the cache monitor.
 * <p>
 * Provides three pages:
 * <ul>
 *     <li><b>Overview</b> — shows embedding model status, model type, model ID, start/stop controls, and AI config button</li>
 *     <li><b>Log</b> — displays the AI service task queue log via {@link org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopRunQueueLoggerPanel}</li>
 *     <li><b>Embedding log</b> — displays the embedding model operation log via {@link org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopProgressLoggerPanel}</li>
 * </ul>
 * <p>
 * Subscribes to {@link org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEventEmitter} on the
 * {@link JIPipeAIServiceComponent.EmbeddingModelService} for event-driven status updates and auto-refreshes on a timer.
 */
public class JIPipeDesktopAIMonitorWindow extends JFrame implements MicroserviceStateChangeListener, JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.TopPill);
    private final List<JIPipeDesktopAIMonitorPage> pages = new ArrayList<>();
    private Timer refreshTimer;

    public JIPipeDesktopAIMonitorWindow(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        this.refreshTimer = new Timer(5000, e -> {
            if (isDisplayable()) {
                refresh();
            } else {
                refreshTimer.stop();
            }
        });
        initialize();
        addPage(new JIPipeDesktopAIMonitorOverviewPage(this), "Overview", JIPipe.RESOURCES.getIcon16("actions/document-preview.png"));
        addPage(new JIPipeDesktopAIMonitorLogPage(this), "Log", JIPipe.RESOURCES.getIcon16("actions/document-preview.png"));
        addPage(new JIPipeDesktopAIMonitorEmbeddingLogPage(this), "Embedding log", JIPipe.RESOURCES.getIcon16("actions/document-preview.png"));
        refresh();
        refreshTimer.start();

        // Subscribe to state change events
        JIPipe.getInstance().getAiService().getEmbeddingModelService().getStateChangeEventEmitter().subscribe(this);
    }

    private void initialize() {
        setTitle(UIUtils.getWindowTitle("AI Monitor"));
        setSize(800, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getJIPipeIcon128());

        JPanel contentPane = new JPanel(new BorderLayout(8, 8));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        tabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        contentPane.add(tabPane, BorderLayout.CENTER);

        setContentPane(contentPane);
    }

    private void addPage(JIPipeDesktopAIMonitorPage page, String name, ImageIcon icon) {
        pages.add(page);
        tabPane.addTab(name, icon, UIUtils.wrapInIslandPanelIfNeeded(page), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
    }

    /**
     * Refreshes all pages.
     */
    public void refresh() {
        for (JIPipeDesktopAIMonitorPage page : pages) {
            page.refresh();
        }
    }

    @Override
    public void onMicroserviceStateChanged(MicroserviceStateChangeEvent event) {
        if (!isDisplayable()) {
            JIPipe.getInstance().getAiService().getEmbeddingModelService().getStateChangeEventEmitter().unsubscribe(this);
            return;
        }
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        JIPipe.getInstance().getAiService().getEmbeddingModelService().getStateChangeEventEmitter().unsubscribe(this);
        super.dispose();
    }
}
