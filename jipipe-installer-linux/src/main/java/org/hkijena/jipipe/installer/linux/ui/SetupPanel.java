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

package org.hkijena.jipipe.installer.linux.ui;

import org.hkijena.jipipe.installer.linux.ui.utils.FormPanel;
import org.hkijena.jipipe.installer.linux.ui.utils.JIPipeRunnerQueue;
import org.hkijena.jipipe.installer.linux.ui.utils.MarkdownDocument;
import org.hkijena.jipipe.installer.linux.ui.utils.MarkdownReader;
import org.hkijena.jipipe.installer.linux.ui.utils.PathEditor;
import org.hkijena.jipipe.installer.linux.ui.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SetupPanel extends JPanel {
    private final MainWindow mainWindow;
    private PathEditor installationPath = new PathEditor(PathEditor.IOMode.Save, PathEditor.PathMode.DirectoriesOnly);
    private JCheckBox createLauncher = new JCheckBox("Create application launcher");

    public SetupPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        initialize();
        updateSettingsFromAPI();
    }

    private void updateSettingsFromAPI() {
        installationPath.setPath(mainWindow.getInstallerRun().getInstallationPath());
        createLauncher.setSelected(mainWindow.getInstallerRun().isCreateLauncher());
    }

    public void initialize() {

        MarkdownReader markdownReader = new MarkdownReader(MarkdownDocument.fromPluginResource("README.md"));
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
        formPanel.addGroupHeader("Installation settings", UIUtils.getIconFromResources("actions/package.png"));
        formPanel.addToForm(installationPath, new JLabel("Installation path"), null);
        formPanel.addWideToForm(createLauncher, null);
        formPanel.addVerticalGlue();

        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, markdownReader, formPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });

        add(splitPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton installButton = new JButton("Install now", UIUtils.getIconFromResources("actions/run-install.png"));
        installButton.addActionListener(e -> installNow());
        toolBar.add(installButton);

        add(toolBar, BorderLayout.SOUTH);
    }

    private void installNow() {
        Path path = installationPath.getPath();
        if(!Files.isDirectory(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Could not create installation path!", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        mainWindow.getInstallerRun().setInstallationPath(path);
        mainWindow.getInstallerRun().setCreateLauncher(createLauncher.isSelected());

        // Setup the run and main window events
        mainWindow.installNow();
    }
}
