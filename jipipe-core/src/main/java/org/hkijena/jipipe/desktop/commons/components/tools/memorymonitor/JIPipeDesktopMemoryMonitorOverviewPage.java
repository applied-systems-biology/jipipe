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

package org.hkijena.jipipe.desktop.commons.components.tools.memorymonitor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.JIPipeMemoryStatus;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tools.JIPipeDesktopMemoryGraph;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class JIPipeDesktopMemoryMonitorOverviewPage extends JIPipeDesktopMemoryMonitorPage {

    private final JLabel usedMemoryLabel = new JLabel();
    private final JLabel maxMemoryLabel = new JLabel();
    private final JLabel usagePercentageLabel = new JLabel();
    private final JIPipeDesktopMemoryGraph memoryGraph;
    private final Timer refreshTimer;

    public JIPipeDesktopMemoryMonitorOverviewPage(JIPipeDesktopMemoryMonitorWindow window) {
        super(window);
        this.memoryGraph = new JIPipeDesktopMemoryGraph(12, true);
        this.refreshTimer = new Timer(5000, this::onRefreshTimer);
        initialize();
        memoryGraph.startRepaintTimer();
        refreshTimer.start();
    }

    private void onRefreshTimer(ActionEvent e) {
        if (isDisplayable()) {
            refresh();
        } else {
            refreshTimer.stop();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        refreshTimer.stop();
    }

    private void initialize() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        add(formPanel, BorderLayout.CENTER);
        memoryGraph.setBorder(UIUtils.createControlBorder());
        memoryGraph.setPreferredSize(new Dimension(400, 200));
        formPanel.addWideToForm(memoryGraph);

        usedMemoryLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        maxMemoryLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        usagePercentageLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        formPanel.addToForm(usedMemoryLabel, new JLabel("Used memory"));
        formPanel.addToForm(maxMemoryLabel, new JLabel("Maximum memory"));
        formPanel.addToForm(usagePercentageLabel, new JLabel("Usage"));

        formPanel.addGroupHeader("Actions", JIPipe.RESOURCES.getIcon16("actions/system-run.png"));
        formPanel.addToForm(new JLabel("Asks the Java runtime to clear unused memory."),
                UIUtils.createButton("Run garbage collector", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), System::gc));
        formPanel.addToForm(new JLabel("Manually refreshes this overview."),
                UIUtils.createButton("Refresh", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"), this::refresh));
    }

    @Override
    public void refresh() {
        JIPipeMemoryStatus current = JIPipe.getMemoryMonitor().getCurrentStatus();

        usedMemoryLabel.setText(StringUtils.formatBytes(current.usedMemory()));
        maxMemoryLabel.setText(StringUtils.formatBytes(current.maxMemory()));
        usagePercentageLabel.setText(String.format("%.2f%%", current.getUsagePercentage()));
    }

    @Override
    public void addToReport(StringBuilder reportBuilder) {
        JIPipeMemoryStatus current = JIPipe.getMemoryMonitor().getCurrentStatus();

        reportBuilder.append("=== Memory Overview ===\n");
        reportBuilder.append("Used memory: ").append(StringUtils.formatBytes(current.usedMemory())).append("\n");
        reportBuilder.append("Maximum memory: ").append(StringUtils.formatBytes(current.maxMemory())).append("\n");
        reportBuilder.append("Usage: ").append(String.format("%.2f%%", current.getUsagePercentage())).append("\n");
        reportBuilder.append("\n");
    }
}
