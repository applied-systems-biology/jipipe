package org.hkijena.jipipe.desktop.commons.components.servers.monitor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.servers.JIPipeServerEvent;
import org.hkijena.jipipe.api.servers.JIPipeServerEventListener;
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
 * Monitor window for external server instances, following a page-based design pattern
 * inspired by the AI and cache monitors.
 *
 * <p>Subscribes to {@link org.hkijena.jipipe.api.servers.JIPipeServerEventEmitter} for
 * event-driven status updates and auto-refreshes on a timer.</p>
 */
public class JIPipeDesktopServerMonitorWindow extends JFrame
        implements JIPipeServerEventListener, JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.TopPill);
    private final List<JIPipeDesktopServerMonitorPage> pages = new ArrayList<>();
    private Timer refreshTimer;

    public JIPipeDesktopServerMonitorWindow(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        this.refreshTimer = new Timer(5000, e -> {
            if (isDisplayable()) {
                refresh();
            } else {
                refreshTimer.stop();
            }
        });
        initialize();
        addPage(new JIPipeDesktopServerMonitorOverviewPage(this), "Overview",
                JIPipe.RESOURCES.getIcon16("actions/document-preview.png"));
        refresh();
        refreshTimer.start();
        JIPipe.getInstance().getServerService()
                .getStateChangedEventEmitter().subscribe(this);
    }

    private void initialize() {
        setTitle(UIUtils.getWindowTitle("Server Monitor"));
        setSize(1000, 700);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getJIPipeIcon128());

        JPanel contentPane = new JPanel(new BorderLayout(8, 8));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        tabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        contentPane.add(tabPane, BorderLayout.CENTER);

        setContentPane(contentPane);
    }

    private void addPage(JIPipeDesktopServerMonitorPage page, String name, ImageIcon icon) {
        pages.add(page);
        tabPane.addTab(name, icon, UIUtils.wrapInIslandPanelIfNeeded(page),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
    }

    /**
     * Refreshes all pages.
     */
    public void refresh() {
        for (JIPipeDesktopServerMonitorPage page : pages) {
            page.refresh();
        }
    }

    @Override
    public void onServerStateChanged(JIPipeServerEvent event) {
        if (!isDisplayable()) {
            JIPipe.getInstance().getServerService()
                    .getStateChangedEventEmitter().unsubscribe(this);
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
        JIPipe.getInstance().getServerService()
                .getStateChangedEventEmitter().unsubscribe(this);
        super.dispose();
    }
}
