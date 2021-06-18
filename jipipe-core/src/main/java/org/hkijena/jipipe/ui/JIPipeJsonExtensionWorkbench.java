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

package org.hkijena.jipipe.ui;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.RecentJsonExtensionsMenu;
import org.hkijena.jipipe.ui.components.ReloadableValidityChecker;
import org.hkijena.jipipe.ui.components.SplashScreen;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.ui.extensionbuilder.JIPipeJsonExtensionContentListUI;
import org.hkijena.jipipe.ui.extensionbuilder.JIPipeJsonExtensionGraphUI;
import org.hkijena.jipipe.ui.extensionbuilder.JIPipeJsonExtensionProjectValidation;
import org.hkijena.jipipe.ui.settings.JIPipeJsonExtensionSettingsUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.scijava.Context;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * UI around a {@link JIPipeJsonExtension}
 */
public class JIPipeJsonExtensionWorkbench extends JPanel implements JIPipeWorkbench {
    private final JIPipeJsonExtensionWindow window;
    private final Context context;
    private final JIPipeJsonExtension project;
    public DocumentTabPane documentTabPane;
    private JLabel statusText;
    private ReloadableValidityChecker validityCheckerPanel;
    private boolean projectModified;

    /**
     * @param window           The parent window
     * @param context          The SciJava context
     * @param project          The project
     * @param showIntroduction if show intro
     */
    public JIPipeJsonExtensionWorkbench(JIPipeJsonExtensionWindow window, Context context, JIPipeJsonExtension project, boolean showIntroduction) {
        this.window = window;
        this.context = context;
        this.project = project;
        initialize(showIntroduction);

        this.project.getEventBus().register(this);
        JIPipe.getInstance().getEventBus().register(this);
        SplashScreen.getInstance().hideSplash();
    }

    /**
     * Informs the user about registered extensions.
     *
     * @param event the event
     */
    @Subscribe
    public void onExtensionRegistered(JIPipe.ExtensionRegisteredEvent event) {
        sendStatusBarText("Registered extension: '" + event.getExtension().getMetadata().getName() + "' with id '" + event.getExtension().getDependencyId() + "'. We recommend to restart ImageJ.");
    }

    private void initialize(boolean showIntroduction) {
        setLayout(new BorderLayout());

        documentTabPane = new DocumentTabPane();
        documentTabPane.addSingletonTab("INTRODUCTION",
                "Introduction",
                UIUtils.getIconFromResources("actions/help-info.png"),
                new JIPipeJsonExtensionInfoUI(this),
                !showIntroduction);
        documentTabPane.addSingletonTab("PROJECT_SETTINGS",
                "Extension settings",
                UIUtils.getIconFromResources("actions/wrench.png"),
                new JIPipeJsonExtensionSettingsUI(this),
                false);
        documentTabPane.addSingletonTab("PROJECT_CONTENTS",
                "Extension contents",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new JIPipeJsonExtensionContentListUI(this),
                false);
        validityCheckerPanel = new ReloadableValidityChecker(new JIPipeJsonExtensionProjectValidation(project));
        documentTabPane.addSingletonTab("VALIDITY_CHECK",
                "Project validation",
                UIUtils.getIconFromResources("actions/checkmark.png"),
                validityCheckerPanel,
                true);
        initializeMenu();
        initializeStatusBar();
        sendStatusBarText("Welcome to the JIPipe extension builder");

        add(documentTabPane, BorderLayout.CENTER);

        if (showIntroduction)
            getDocumentTabPane().selectSingletonTab("INTRODUCTION");
        else
            getDocumentTabPane().selectSingletonTab("PROJECT_CONTENTS");
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

    /**
     * Sends a text to the status bar
     *
     * @param text The text
     */
    @Override
    public void sendStatusBarText(String text) {
        LocalDateTime localDateTime = LocalDateTime.now();
        statusText.setText(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + text);
    }

    private void initializeMenu() {
        JMenuBar menu = new JMenuBar();
        menu.setBorderPainted(false);

        JMenu projectMenu = new JMenu("Extension");

        // Add "New project" toolbar entry
        JMenuItem newProjectButton = new JMenuItem("New", UIUtils.getIconFromResources("actions/document-new.png"));
        newProjectButton.setToolTipText("Creates a new extension");
        newProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        newProjectButton.addActionListener(e -> window.newProject(false));
        projectMenu.add(newProjectButton);

        UIUtils.installMenuExtension(this, projectMenu, MenuTarget.ExtensionBuilderMainMenu, true);
        projectMenu.addSeparator();

        // "Open project" entry
        JMenuItem openProjectButton = new JMenuItem("Open ...", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openProjectButton.setToolTipText("Opens an extension");
        openProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openProjectButton.addActionListener(e -> window.openProject());
        projectMenu.add(openProjectButton);

        // Recent projects entry
        projectMenu.add(new RecentJsonExtensionsMenu("Recent extensions", UIUtils.getIconFromResources("actions/clock.png"), (JIPipeJsonExtensionWindow) getWindow()));

        projectMenu.addSeparator();

        // "Save project" entry
        JMenuItem saveProjectButton = new JMenuItem("Save ...", UIUtils.getIconFromResources("actions/save.png"));
        saveProjectButton.setToolTipText("Saves the extension. If the extension was opened from a file or previously saved, the file will be overwritten.");
        saveProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveProjectButton.addActionListener(e -> window.saveProjectAs(true));
        projectMenu.add(saveProjectButton);

        // "Save project" entry
        JMenuItem saveProjectAsButton = new JMenuItem("Save as ...", UIUtils.getIconFromResources("actions/save.png"));
        saveProjectAsButton.setToolTipText("Saves the extension to a new file.");
        saveProjectAsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
        saveProjectAsButton.addActionListener(e -> window.saveProjectAs(false));
        projectMenu.add(saveProjectAsButton);

        menu.add(projectMenu);

        projectMenu.addSeparator();

        JMenuItem projectSettingsButton = new JMenuItem("Extension settings", UIUtils.getIconFromResources("actions/wrench.png"));
        projectSettingsButton.addActionListener(e -> openExtensionSettings());
        projectMenu.add(projectSettingsButton);

        JMenuItem projectContentButton = new JMenuItem("Extension contents", UIUtils.getIconFromResources("actions/plugins.png"));
        projectContentButton.addActionListener(e -> openExtensionContents());
        projectMenu.add(projectContentButton);

        // Tools menu
        JMenu toolsMenu = new JMenu("Tools");
        UIUtils.installMenuExtension(this, toolsMenu, MenuTarget.ExtensionBuilderToolsMenu, false);
        if (toolsMenu.getItemCount() > 0)
            menu.add(toolsMenu);

        menu.add(Box.createHorizontalGlue());

        // "Validate" entry
        JButton validateProjectButton = new JButton("Validate", UIUtils.getIconFromResources("actions/checkmark.png"));
        validateProjectButton.setToolTipText("Opens a new tab to check parameters and graph for validity.");
        validateProjectButton.addActionListener(e -> validateProject());
        UIUtils.makeFlat(validateProjectButton);
        menu.add(validateProjectButton);

        // "Run" entry
        JButton installButton = new JButton("Install", UIUtils.getIconFromResources("actions/run-install.png"));
        installButton.setToolTipText("Installs the current extension.");
        UIUtils.makeFlat(installButton);
        installButton.addActionListener(e -> installProject());
        menu.add(installButton);

        // "Help" entry
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setIcon(UIUtils.getIconFromResources("actions/help.png"));
        JMenuItem quickHelp = new JMenuItem("Getting started", UIUtils.getIconFromResources("actions/help-info.png"));
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab("INTRODUCTION"));
        helpMenu.add(quickHelp);

        UIUtils.installMenuExtension(this, helpMenu, MenuTarget.ExtensionHelpMenu, true);

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
        JIPipeValidityReport report = validityCheckerPanel.getReport();
        if (!report.isValid()) {
            validateProject();
            if (JOptionPane.showConfirmDialog(this, "The extension builder found potential issues with the extension. Install anyways?",
                    "Install extension",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
                return;
        }

        JIPipeJsonExtensionWindow.installExtension(this, getProject(), true, true);
    }

    /**
     * @return The window that contains the UI
     */
    public Window getWindow() {
        return window;
    }

    /**
     * @return The extension project
     */
    public JIPipeJsonExtension getProject() {
        return project;
    }

    /**
     * @return The SciJava context
     */
    @Override
    public Context getContext() {
        return context;
    }

    /**
     * @return The tab pane
     */
    @Override
    public DocumentTabPane getDocumentTabPane() {
        return documentTabPane;
    }

    /**
     * Triggered when content was removed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onContentRemovedEvent(JIPipe.ExtensionContentRemovedEvent event) {
        removeUnnecessaryAlgorithmGraphEditors();
    }

    private void removeUnnecessaryAlgorithmGraphEditors() {
        Set<DocumentTabPane.DocumentTab> toRemove = new HashSet<>();
        for (DocumentTabPane.DocumentTab tab : getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeJsonExtensionGraphUI) {
                JIPipeJsonExtensionGraphUI graphUI = (JIPipeJsonExtensionGraphUI) tab.getContent();
                boolean notFound = project.getNodeInfos().stream().noneMatch(d -> d.getGraph() == graphUI.getAlgorithmGraph());
                if (notFound) {
                    toRemove.add(tab);
                }
            }
        }
        for (DocumentTabPane.DocumentTab documentTab : toRemove) {
            getDocumentTabPane().forceCloseTab(documentTab);
        }
    }

    @Override
    public boolean isProjectModified() {
        return projectModified;
    }

    @Override
    public void setProjectModified(boolean projectModified) {
        this.projectModified = projectModified;
    }
}
