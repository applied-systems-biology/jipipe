package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.ui.components.*;
import org.hkijena.acaq5.ui.extensionbuilder.ACAQJsonExtensionContentListUI;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.ACAQTraitGraphUI;
import org.hkijena.acaq5.ui.settings.ACAQJsonExtensionSettingsUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ACAQJsonExtensionUI extends JPanel {
    private final ACAQJsonExtensionWindow window;
    private final ACAQGUICommand command;
    private final ACAQJsonExtension project;
    public DocumentTabPane documentTabPane;
    private JLabel statusText;
    private ReloadableValidityChecker validityCheckerPanel;

    public ACAQJsonExtensionUI(ACAQJsonExtensionWindow window, ACAQGUICommand command, ACAQJsonExtension project) {
        this.window = window;
        this.command = command;
        this.project = project;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        documentTabPane = new DocumentTabPane();
        documentTabPane.addSingletonTab("INTRODUCTION",
                "Introduction",
                UIUtils.getIconFromResources("info.png"),
                new MarkdownReader(true, MarkdownDocument.fromPluginResource("documentation/introduction-extension-builder.md")),
                false);
        documentTabPane.addSingletonTab("PROJECT_SETTINGS",
                "Extension settings",
                UIUtils.getIconFromResources("wrench.png"),
                new ACAQJsonExtensionSettingsUI(this),
                false);
        documentTabPane.addSingletonTab("PROJECT_CONTENTS",
                "Extension contents",
                UIUtils.getIconFromResources("module.png"),
                new ACAQJsonExtensionContentListUI(this),
                false);
        documentTabPane.addSingletonTab("TRAIT_GRAPH",
                "Annotations",
                UIUtils.getIconFromResources("connect.png"),
                new ACAQTraitGraphUI(this),
                false);
        validityCheckerPanel = new ReloadableValidityChecker(project);
        documentTabPane.addSingletonTab("VALIDITY_CHECK",
                "Project validation",
                UIUtils.getIconFromResources("checkmark.png"),
                validityCheckerPanel,
                true);
        initializeMenu();
        initializeStatusBar();
        sendStatusBarText("Welcome to the ACAQ5 extension builder");

        add(documentTabPane, BorderLayout.CENTER);
    }

    private void validateProject() {
        validityCheckerPanel.recheckValidity();
        documentTabPane.selectSingletonTab("VALIDITY_CHECK");
    }

    private void initializeStatusBar() {
        JXStatusBar statusBar = new JXStatusBar();
        statusText = new JLabel("Ready ...");
        statusBar.add(statusText);
        add(statusBar, BorderLayout.SOUTH);
    }

    public void sendStatusBarText(String text) {
        LocalDateTime localDateTime = LocalDateTime.now();
        statusText.setText(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + text);
    }

    private void initializeMenu() {
        JMenuBar menu = new JMenuBar();

        JMenu projectMenu = new JMenu("Extension");

        // Add "New project" toolbar entry
        JMenuItem newProjectButton = new JMenuItem("New", UIUtils.getIconFromResources("new.png"));
        newProjectButton.setToolTipText("Creates a new extension");
        newProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        newProjectButton.addActionListener(e -> window.newProject());
        projectMenu.add(newProjectButton);

        projectMenu.addSeparator();

        // "Open project" entry
        JMenuItem openProjectButton = new JMenuItem("Open ...", UIUtils.getIconFromResources("open.png"));
        openProjectButton.setToolTipText("Opens an extension");
        openProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openProjectButton.addActionListener(e -> window.openProject());
        projectMenu.add(openProjectButton);

        // Recent projects entry
        projectMenu.add(new RecentJsonExtensionsMenu("Recent extensions", UIUtils.getIconFromResources("clock.png"), getWindow()));

        projectMenu.addSeparator();

        // "Save project" entry
        JMenuItem saveProjectButton = new JMenuItem("Save ...", UIUtils.getIconFromResources("save.png"));
        saveProjectButton.setToolTipText("Saves the extension. If the extension was opened from a file or previously saved, the file will be overwritten.");
        saveProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveProjectButton.addActionListener(e -> window.saveProjectAs(true));
        projectMenu.add(saveProjectButton);

        // "Save project" entry
        JMenuItem saveProjectAsButton = new JMenuItem("Save as ...", UIUtils.getIconFromResources("save.png"));
        saveProjectAsButton.setToolTipText("Saves the extension to a new file.");
        saveProjectAsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
        saveProjectAsButton.addActionListener(e -> window.saveProjectAs(false));
        projectMenu.add(saveProjectAsButton);

        menu.add(projectMenu);

        projectMenu.addSeparator();

        JMenuItem projectSettingsButton = new JMenuItem("Extension settings", UIUtils.getIconFromResources("wrench.png"));
        projectSettingsButton.addActionListener(e -> openExtensionSettings());
        projectMenu.add(projectSettingsButton);

        JMenuItem projectContentButton = new JMenuItem("Extension contents", UIUtils.getIconFromResources("module.png"));
        projectContentButton.addActionListener(e -> openExtensionContents());
        projectMenu.add(projectContentButton);

        menu.add(Box.createHorizontalGlue());

        // "Validate" entry
        JButton validateProjectButton = new JButton("Validate", UIUtils.getIconFromResources("checkmark.png"));
        validateProjectButton.setToolTipText("Opens a new tab to check parameters and graph for validity.");
        validateProjectButton.addActionListener(e -> validateProject());
        UIUtils.makeFlat(validateProjectButton);
        menu.add(validateProjectButton);

        // "Run" entry
        JButton installButton = new JButton("Install", UIUtils.getIconFromResources("download.png"));
        installButton.setToolTipText("Installs the current extension.");
        UIUtils.makeFlat(installButton);
        installButton.addActionListener(e -> installProject());
        menu.add(installButton);

        // "Help" entry
        JMenu helpMenu = new JMenu();
        helpMenu.setIcon(UIUtils.getIconFromResources("help.png"));
        JMenuItem quickHelp = new JMenuItem("Quick introduction", UIUtils.getIconFromResources("quickload.png"));
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab("INTRODUCTION"));
        helpMenu.add(quickHelp);
        menu.add(helpMenu);

        add(menu, BorderLayout.NORTH);
    }

    private void openExtensionContents() {
        documentTabPane.selectSingletonTab("PROJECT_CONTENTS");
    }

    private void openExtensionSettings() {
        documentTabPane.selectSingletonTab("PROJECT_SETTINGS");
    }

    private void installProject() {
        validityCheckerPanel.recheckValidity();
        ACAQValidityReport report = validityCheckerPanel.getReport();
        if (!report.isValid()) {
            validateProject();
        } else {

        }
    }

    public ACAQJsonExtensionWindow getWindow() {
        return window;
    }

    public ACAQJsonExtension getProject() {
        return project;
    }

    public Context getContext() {
        return command.getContext();
    }

    public DocumentTabPane getDocumentTabPane() {
        return documentTabPane;
    }
}
