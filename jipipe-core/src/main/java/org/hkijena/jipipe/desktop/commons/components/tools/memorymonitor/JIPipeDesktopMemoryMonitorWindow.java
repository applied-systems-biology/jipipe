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
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JIPipeDesktopMemoryMonitorWindow extends JFrame implements JIPipeDesktopWorkbenchAccess {
    private final JIPipeDesktopWorkbench workbench;
    private final JToggleButton autoRefreshToggle = new JToggleButton();
    private final List<JIPipeDesktopMemoryMonitorPage> pages = new ArrayList<>();
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.TopPill);
    private Timer refreshTimer;

    public JIPipeDesktopMemoryMonitorWindow(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        this.autoRefreshToggle.setSelected(true);
        this.refreshTimer = new Timer(5000, e -> {
            if (isDisplayable()) {
                if (autoRefreshToggle.isSelected()) {
                    refresh();
                }
            } else {
                refreshTimer.stop();
            }
        });
        initialize();
        addPage(new JIPipeDesktopMemoryMonitorOverviewPage(this), "Overview", JIPipe.RESOURCES.getIcon16("actions/document-preview.png"));
        refresh();
        refreshTimer.start();
    }

    private void initialize() {
        setTitle(UIUtils.getWindowTitle("Memory monitor"));
        setIconImage(UIUtils.getJIPipeIcon128());

        JPanel contentPane = new JPanel(new BorderLayout(8, 8));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        initializeStatusBar(contentPane);

        tabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        contentPane.add(tabPane, BorderLayout.CENTER);

        setContentPane(contentPane);
    }

    private void initializeStatusBar(JPanel contentPane) {
        JXStatusBar statusBar = new JXStatusBar();
        statusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        statusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));

        UIUtils.makeButtonFlat25x25(autoRefreshToggle);
        statusBar.add(autoRefreshToggle);
        autoRefreshToggle.setIcon(JIPipe.RESOURCES.getIcon16("actions/quickopen.png"));
        UIUtils.makeNonOpaque(statusBar, true);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        JButton refreshButton = UIUtils.createButton("", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"), this::refresh);
        UIUtils.makeButtonFlat25x25(refreshButton);
        statusBar.add(refreshButton);

        JButton saveReportButton = UIUtils.createButton("", JIPipe.RESOURCES.getIcon16("actions/save.png"), this::saveReport);
        saveReportButton.setToolTipText("Save report to text file");
        UIUtils.makeButtonFlat25x25(saveReportButton);
        statusBar.add(saveReportButton);
    }

    private void addPage(JIPipeDesktopMemoryMonitorPage page, String name, ImageIcon icon) {
        pages.add(page);
        tabPane.addTab(name, icon, UIUtils.wrapInIslandPanelIfNeeded(page), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
    }

    public void refresh() {
        for (JIPipeDesktopMemoryMonitorPage page : pages) {
            page.refresh();
        }
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    /**
     * Generates a report of the current memory monitor state.
     *
     * @return the report content as a string
     */
    public String generateReport() {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("=== JIPipe Memory Monitor Report ===\n");
        reportBuilder.append("Generated: ").append(DateTimeFormatter.ISO_INSTANT.format(Instant.now())).append("\n\n");

        for (JIPipeDesktopMemoryMonitorPage page : pages) {
            page.addToReport(reportBuilder);
        }

        return reportBuilder.toString();
    }

    /**
     * Saves the report to a text file.
     */
    private void saveReport() {
        Path savedPath = JIPipeDesktop.saveFile(this, workbench,
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data,
                "Save Memory Monitor Report",
                HTMLText.EMPTY,
                PathUtils.EXTENSION_FILTER_TXT);
        if (savedPath != null) {
            try {
                Files.writeString(savedPath, generateReport());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save report: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
