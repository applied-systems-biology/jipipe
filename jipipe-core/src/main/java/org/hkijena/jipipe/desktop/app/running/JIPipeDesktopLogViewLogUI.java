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

package org.hkijena.jipipe.desktop.app.running;

import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopGenericNotificationInboxUI;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.plugins.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JIPipeDesktopLogViewLogUI extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeRunnableLogEntry logEntry;

    public JIPipeDesktopLogViewLogUI(JIPipeDesktopWorkbench workbench, JIPipeRunnableLogEntry logEntry) {
        super(workbench);
        this.logEntry = logEntry;
        initialize();
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
        JTextPane logReader = new JTextPane();
        logReader.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logReader);
        tabPane.addTab("Log", UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"), scrollPane, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        logReader.setText(logEntry.getLog());

        // Init notifications
        tabPane.addTab("Notifications",
                logEntry.getNotifications().isEmpty() ? UIUtils.getIcon32FromResources("actions/notifications.png") : UIUtils.getIcon32FromResources("status/dialog-error.png"),
                new JIPipeDesktopGenericNotificationInboxUI(getDesktopWorkbench(), logEntry.getNotifications()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Init toolbar
        toolBar.add(Box.createHorizontalGlue());

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        exportButton.addActionListener(e -> exportLog());
        toolBar.add(exportButton);

        JButton openInExternalToolButton = new JButton("Open in external editor", UIUtils.getIconFromResources("actions/open-in-new-window.png"));
        openInExternalToolButton.addActionListener(e -> openLogInExternalTool());
        toolBar.add(openInExternalToolButton);

        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    private void openLogInExternalTool() {
        Path tempFile = RuntimeSettings.generateTempFile("log", ".txt");
        try {
            Files.write(tempFile, logEntry.getLog().getBytes(StandardCharsets.UTF_8));
            Desktop.getDesktop().open(tempFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void exportLog() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export log", UIUtils.EXTENSION_FILTER_TXT);
        if (path != null) {
            try {
                Files.write(path, logEntry.getLog().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
