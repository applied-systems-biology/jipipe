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

package org.hkijena.jipipe.desktop.jsonextensionbuilder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopJsonExtensionSettingsUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopRecentJsonExtensionsMenu;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopReloadableValidityChecker;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopSplashScreen;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder.JIPipeDesktopJsonExtensionContentListUI;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder.JIPipeDesktopJsonExtensionGraphUI;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder.JIPipeDesktopJsonExtensionProjectValidation;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * UI around a {@link JIPipeJsonPlugin}
 */
public class JIPipeDesktopJsonExtensionWorkbench extends JPanel implements JIPipeDesktopWorkbench, JIPipeService.ExtensionRegisteredEventListener, JIPipeService.ExtensionContentRemovedEventListener {
    private final JIPipeDesktopJsonExtensionWindow window;
    private final Context context;
    private final JIPipeJsonPlugin pluginProject;
    public JIPipeDesktopTabPane documentTabPane;
    private JLabel statusText;
    private JIPipeDesktopReloadableValidityChecker validityCheckerPanel;
    private boolean projectModified;
    private JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();


    /**
     * @param window           The parent window
     * @param context          The SciJava context
     * @param pluginProject    The project
     * @param showIntroduction if show intro
     */
    public JIPipeDesktopJsonExtensionWorkbench(JIPipeDesktopJsonExtensionWindow window, Context context, JIPipeJsonPlugin pluginProject, boolean showIntroduction) {
        this.window = window;
        this.context = context;
        this.pluginProject = pluginProject;
        initialize(showIntroduction);

        this.pluginProject.getExtensionContentRemovedEventEmitter().subscribeWeak(this);
        JIPipe.getInstance().getExtensionRegisteredEventEmitter().subscribeWeak(this);
        JIPipeDesktopSplashScreen.getInstance().hideSplash();
    }

    /**
     * Informs the user about registered extensions.
     *
     * @param event the event
     */
    @Override
    public void onJIPipeExtensionRegistered(JIPipe.ExtensionRegisteredEvent event) {
        sendStatusBarText("Registered extension: '" + event.getExtension().getMetadata().getName() + "' with id '" + event.getExtension().getDependencyId() + "'. We recommend to restart ImageJ.");
    }

    private void initialize(boolean showIntroduction) {
        setLayout(new BorderLayout());

        documentTabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
        documentTabPane.registerSingletonTab("INTRODUCTION",
                "Introduction",
                UIUtils.getIconFromResources("actions/help-info.png"),
                () -> new JIPipeDesktopDesktopJsonExtensionInfoUI(this),
                showIntroduction ? JIPipeDesktopTabPane.SingletonTabMode.Present : JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab("PROJECT_SETTINGS",
                "Extension settings",
                UIUtils.getIconFromResources("actions/wrench.png"),
                () -> new JIPipeDesktopJsonExtensionSettingsUI(this),
                JIPipeDesktopTabPane.SingletonTabMode.Present);
        documentTabPane.registerSingletonTab("PROJECT_CONTENTS",
                "Extension contents",
                UIUtils.getIconFromResources("actions/plugins.png"),
                () -> new JIPipeDesktopJsonExtensionContentListUI(this),
                JIPipeDesktopTabPane.SingletonTabMode.Present);
        validityCheckerPanel = new JIPipeDesktopReloadableValidityChecker(this, new JIPipeDesktopJsonExtensionProjectValidation(pluginProject));
        documentTabPane.registerSingletonTab("VALIDITY_CHECK",
                "Project validation",
                UIUtils.getIconFromResources("actions/checkmark.png"),
                () -> validityCheckerPanel,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
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

        UIUtils.installMenuExtension(this, projectMenu, JIPipeMenuExtensionTarget.ExtensionBuilderMainMenu, true);
        projectMenu.addSeparator();

        // "Open project" entry
        JMenuItem openProjectButton = new JMenuItem("Open ...", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openProjectButton.setToolTipText("Opens an extension");
        openProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openProjectButton.addActionListener(e -> window.openProject());
        projectMenu.add(openProjectButton);

        // Recent projects entry
        projectMenu.add(new JIPipeDesktopRecentJsonExtensionsMenu("Recent extensions", UIUtils.getIconFromResources("actions/clock.png"), (JIPipeDesktopJsonExtensionWindow) getWindow()));

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
        UIUtils.installMenuExtension(this, toolsMenu, JIPipeMenuExtensionTarget.ExtensionBuilderToolsMenu, false);
        if (toolsMenu.getItemCount() > 0)
            menu.add(toolsMenu);

        menu.add(Box.createHorizontalGlue());

        // "Validate" entry
        JButton validateProjectButton = new JButton("Validate", UIUtils.getIconFromResources("actions/checkmark.png"));
        validateProjectButton.setToolTipText("Opens a new tab to check parameters and graph for validity.");
        validateProjectButton.addActionListener(e -> validateProject());
        UIUtils.setStandardButtonBorder(validateProjectButton);
        menu.add(validateProjectButton);

        // "Run" entry
        JButton installButton = new JButton("Install", UIUtils.getIconFromResources("actions/run-install.png"));
        installButton.setToolTipText("Installs the current extension.");
        UIUtils.setStandardButtonBorder(installButton);
        installButton.addActionListener(e -> installProject());
        menu.add(installButton);

        // "Help" entry
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setIcon(UIUtils.getIconFromResources("actions/help.png"));
        JMenuItem quickHelp = new JMenuItem("Getting started", UIUtils.getIconFromResources("actions/help-info.png"));
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab("INTRODUCTION"));
        helpMenu.add(quickHelp);

        UIUtils.installMenuExtension(this, helpMenu, JIPipeMenuExtensionTarget.ExtensionHelpMenu, true);

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
        JIPipeValidationReport report = validityCheckerPanel.getReport();
        if (!report.isValid()) {
            validateProject();
            if (JOptionPane.showConfirmDialog(this, "The extension builder found potential issues with the extension. Install anyway?",
                    "Install extension",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
                return;
        }

        JIPipeDesktopJsonExtensionWindow.installExtension(this, getPluginProject(), true, true);
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
    public JIPipeJsonPlugin getPluginProject() {
        return pluginProject;
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
    public JIPipeDesktopTabPane getDocumentTabPane() {
        return documentTabPane;
    }

    private void removeUnnecessaryAlgorithmGraphEditors() {
        Set<JIPipeDesktopTabPane.DocumentTab> toRemove = new HashSet<>();
        for (JIPipeDesktopTabPane.DocumentTab tab : getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeDesktopJsonExtensionGraphUI) {
                JIPipeDesktopJsonExtensionGraphUI graphUI = (JIPipeDesktopJsonExtensionGraphUI) tab.getContent();
                boolean notFound = pluginProject.getNodeInfos().stream().noneMatch(d -> d.getGraph() == graphUI.getGraph());
                if (notFound) {
                    toRemove.add(tab);
                }
            }
        }
        for (JIPipeDesktopTabPane.DocumentTab documentTab : toRemove) {
            getDocumentTabPane().forceCloseTab(documentTab);
        }
    }

    public boolean isProjectModified() {
        return projectModified;
    }

    public void setProjectModified(boolean projectModified) {
        this.projectModified = projectModified;
    }

    @Override
    public JIPipeNotificationInbox getNotificationInbox() {
        return notificationInbox;
    }

    @Override
    public void showMessageDialog(String message, String title) {

    }

    @Override
    public void showErrorDialog(String message, String title) {

    }

    @Override
    public JIPipeProject getProject() {
        return null;
    }

    @Override
    public void onJIPipeExtensionContentRemoved(JIPipeService.ExtensionContentRemovedEvent event) {
        removeUnnecessaryAlgorithmGraphEditors();
    }
}
