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

package org.hkijena.jipipe.desktop.app.running.logs;

import com.google.common.base.CharMatcher;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopGenericNotificationInboxUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JIPipeDesktopLogViewLogUI extends JIPipeDesktopWorkbenchPanel {
    public static int LOG_LINE_LIMIT = 500;
    private final JIPipeRunnableLogEntry logEntry;
    private final JCheckBox displayFullLog = new JCheckBox("Show full log");
    private final JTextPane logReader = new JTextPane();
    private JScrollPane scrollPane;

    public JIPipeDesktopLogViewLogUI(JIPipeDesktopWorkbench workbench, JIPipeRunnableLogEntry logEntry) {
        super(workbench);
        this.logEntry = logEntry;
        initialize();
        reloadLog();
    }

    private void reloadLog() {
        if (displayFullLog.isSelected() || CharMatcher.is('\n').countIn(logEntry.getLog()) < LOG_LINE_LIMIT) {
            logReader.setText(logEntry.getLog());
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
            builder.append("Only displaying the last ").append(LOG_LINE_LIMIT).append(" lines\n\n\n");
            String[] lines = logEntry.getLog().split("\n");
            for (int i = Math.max(0, lines.length - LOG_LINE_LIMIT); i < lines.length; ++i) {
                builder.append(lines[i]).append("\n");
            }
            logReader.setText(builder.toString());
        }
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()));
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Init base layout
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        add(toolBar, BorderLayout.NORTH);
        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);
        add(tabPane, BorderLayout.CENTER);

        // Init reader
        logReader.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        scrollPane = new JScrollPane(logReader);
        tabPane.addTab("Log", UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"), scrollPane, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Init notifications
        tabPane.addTab("Notifications",
                logEntry.getNotifications().isEmpty() ? UIUtils.getIcon32FromResources("actions/notifications.png") : UIUtils.getIcon32FromResources("status/dialog-error.png"),
                new JIPipeDesktopGenericNotificationInboxUI(getDesktopWorkbench(), logEntry.getNotifications()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Init toolbar
        toolBar.add(displayFullLog);
        displayFullLog.addActionListener(e -> reloadLog());

        toolBar.add(Box.createHorizontalGlue());

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        exportButton.addActionListener(e -> exportLog());
        toolBar.add(exportButton);

        JButton openInExternalToolButton = new JButton("Open in external editor", UIUtils.getIconFromResources("actions/open-in-new-window.png"));
        openInExternalToolButton.addActionListener(e -> openLogInExternalTool());
        toolBar.add(openInExternalToolButton);
    }

    private void openLogInExternalTool() {
        Path tempFile = JIPipeRuntimeApplicationSettings.getTemporaryFile("log", ".txt");
        try {
            Files.write(tempFile, logEntry.getLog().getBytes(StandardCharsets.UTF_8));
            Desktop.getDesktop().open(tempFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void exportLog() {
        Path path = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export log", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_TXT);
        if (path != null) {
            try {
                Files.write(path, logEntry.getLog().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
