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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.parameterreference.JIPipeDesktopGraphNodeParameterReferenceGroupCollectionEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopImageFrameComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditorKit;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.SizeFitMode;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 * UI that gives an overview of a pipeline (shows parameters, etc.)
 */
public class JIPipeDesktopProjectOverviewUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JTextPane descriptionReader;
    private final JScrollPane descriptionReaderScrollPane;

    private final JIPipeDesktopProjectSettingsComponents projectSettingsComponents;
    private final JPanel runtimePartitionsPanel;
    private final JIPipeDesktopParameterFormPanel userParametersPanel;
    private final JIPipeDesktopParameterFormPanel userDirectoriesPanel;

    private JTextField licenseInfo;
    private JTextField projectName;
    private JTextField projectStats;
    private JPanel projectAuthors;
    private JIPipeDesktopImageFrameComponent headerPanel;
    private JButton openWebsiteButton;
    private JButton copyCitationButton;
    private JButton copyDependencyCitationsButton;
    private JButton showAcknowledgedAuthorsButton;
    private JIPipeDesktopSplitPane splitPane;

    /**
     * Creates a new instance
     *
     * @param workbench The workbench UI
     */
    public JIPipeDesktopProjectOverviewUI(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);

        // Description
        descriptionReader = new JTextPane();
        descriptionReader.setContentType("text/html");
        descriptionReader.setEditorKit(new JIPipeDesktopHTMLEditorKit());
        descriptionReader.setEditable(false);
        UIUtils.registerHyperlinkHandler(descriptionReader);
        descriptionReaderScrollPane = new JScrollPane(descriptionReader);

        // Project settings
        projectSettingsComponents = new JIPipeDesktopProjectSettingsComponents(getProject(), workbench);

        userParametersPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                getProject().getPipelineParameters(),
                MarkdownText.fromPluginResource("documentation/project-info-parameters.md", new HashMap<>()),
                JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING
                        | JIPipeDesktopParameterFormPanel.NO_EMPTY_GROUP_HEADERS | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION |
                        JIPipeDesktopParameterFormPanel.DOCUMENTATION_BELOW);
        userDirectoriesPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                getProject().getMetadata().getDirectories(),
                MarkdownText.fromPluginResource("documentation/project-info-directories.md", new HashMap<>()),
                JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING
                        | JIPipeDesktopParameterFormPanel.NO_EMPTY_GROUP_HEADERS | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION |
                        JIPipeDesktopParameterFormPanel.DOCUMENTATION_BELOW);
        runtimePartitionsPanel = new JPanel(new BorderLayout());

        initialize();
        refreshAll();
        getProject().getMetadata().getParameterChangedEventEmitter().subscribeWeak(this);

    }

    private void refreshAll() {
        renderBackgroundPanel();
        refreshDescription();
        refreshHeaderText();
        refreshTechnicalInfo();
        refreshHeaderButtons();
        userParametersPanel.reloadForm();
        userDirectoriesPanel.reloadForm();
    }

    private void refreshHeaderText() {
        projectName.setText(StringUtils.orElse(getProject().getMetadata().getName(), "Unnamed project"));
        projectAuthors.removeAll();
        for (JIPipeAuthorMetadata author : getProject().getMetadata().getAuthors()) {
            JButton authorButton = new JButton(author.toString(), UIUtils.getIconFromResources("actions/im-user.png"));
            authorButton.setToolTipText("Click to show more information");
            authorButton.addActionListener(e -> {
                JIPipeAuthorMetadata.openAuthorInfoWindow(getDesktopWorkbench().getWindow(), getProject().getMetadata().getAuthors(), author);
            });
            authorButton.setOpaque(false);
            authorButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            authorButton.setBackground(new Color(0, 0, 0, 0));
            projectAuthors.add(authorButton);
        }
        projectAuthors.revalidate();
        projectAuthors.repaint();
    }


    private void refreshHeaderButtons() {
        if (!StringUtils.isNullOrEmpty(getProject().getMetadata().getWebsite())) {
            openWebsiteButton.setToolTipText(getProject().getMetadata().getWebsite());
            openWebsiteButton.setEnabled(true);
        } else {
            openWebsiteButton.setToolTipText("No website provided");
            openWebsiteButton.setEnabled(false);
        }
        if (!StringUtils.isNullOrEmpty(getProject().getMetadata().getCitation())) {
            copyCitationButton.setToolTipText(getProject().getMetadata().getCitation());
            copyCitationButton.setEnabled(true);
        } else {
            copyCitationButton.setToolTipText("No citation provided");
            copyCitationButton.setEnabled(false);
        }
        if (getProject().getMetadata().getDependencyCitations().isEmpty()) {
            copyDependencyCitationsButton.setToolTipText("No cited sources provided");
            copyDependencyCitationsButton.setEnabled(false);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (String dependencyCitation : getProject().getMetadata().getDependencyCitations()) {
                stringBuilder.append(dependencyCitation).append("\n\n");
            }
            copyDependencyCitationsButton.setToolTipText(stringBuilder.toString());
            copyDependencyCitationsButton.setEnabled(true);
        }
        showAcknowledgedAuthorsButton.setVisible(!getProject().getMetadata().getAcknowledgements().isEmpty());
    }

    private void refreshTechnicalInfo() {
        licenseInfo.setText(StringUtils.orElse(getProject().getMetadata().getLicense(), "No license"));
        projectStats.setText(getProject().getGraph().getNodeCount() + " nodes in " + getProject().getCompartments().size() + " compartments");
    }

    private void renderBackgroundPanel() {
        BufferedImage headerBackground;
        headerBackground = UIUtils.getHeaderPanelBackground();
        headerPanel.setBackgroundImage(headerBackground);
    }

    private void refreshDescription() {
        descriptionReader.setText(getProject().getMetadata().getDescription().getHtml());
    }

    private void initialize() {
        setLayout(new BorderLayout());

        initializeRuntimePartitionsPanel();
        initializeUserParametersPanel();
        initializeHeaderPanel();

        descriptionReaderScrollPane.setBorder(null);
        userParametersPanel.getScrollPane().setBorder(null);

        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Right);
        tabPane.registerSingletonTab("PARAMETERS",
                "Parameters",
                UIUtils.getIcon32FromResources("actions/configure.png"),
                () -> userParametersPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Selected);
        tabPane.registerSingletonTab(
                "DIRECTORIES",
                "Directories",
                UIUtils.getIcon32FromResources("actions/stock_folder-copy.png"),
                () -> userDirectoriesPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
        tabPane.registerSingletonTab(
                "PARTITIONS",
                "Partitions",
                UIUtils.getIcon32FromResources("actions/runtime-partition.png"),
                () -> runtimePartitionsPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
        tabPane.registerSingletonTab(
                "BOOKMARKS",
                "Bookmarks",
                UIUtils.getIcon32FromResources("actions/bookmark.png"),
                () -> new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getProject().getGraph(), null, null),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
        tabPane.registerSingletonTab(
                "SETTINGS",
                "Settings",
                UIUtils.getIcon32FromResources("actions/wrench.png"),
                projectSettingsComponents::getTreePanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);

        splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT, descriptionReaderScrollPane, tabPane, new JIPipeDesktopSplitPane.DynamicSidebarRatio(600, false));
        add(splitPane, BorderLayout.CENTER);

        tabPane.getTabbedPane().addChangeListener(e -> {
            if ("SETTINGS".equals(tabPane.getCurrentlySelectedSingletonTabId())) {
                switchToSettings();
            } else {
                switchToDescription();
            }
        });
    }

    private void switchToDescription() {
        splitPane.setLeftComponent(descriptionReaderScrollPane);
        splitPane.revalidate();
        splitPane.applyRatio();
        splitPane.repaint();
    }

    private void switchToSettings() {
        splitPane.setLeftComponent(projectSettingsComponents.getContentPanel());
        splitPane.revalidate();
        splitPane.applyRatio();
        splitPane.repaint();
    }

    private void initializeRuntimePartitionsPanel() {
        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.TOP_BOTTOM,
                new JIPipeDesktopRuntimePartitionListEditor(getDesktopProjectWorkbench()),
                new JIPipeDesktopMarkdownReader(false, MarkdownText.fromPluginResource("documentation/project-info-runtime-partitions.md")),
                JIPipeDesktopSplitPane.RATIO_3_TO_1);
        runtimePartitionsPanel.add(splitPane, BorderLayout.CENTER);
    }

    private void initializeUserParametersPanel() {
        JButton editButton = new JButton("Edit", UIUtils.getIconFromResources("actions/edit.png"));
        editButton.setToolTipText("Configures the list of available pipeline parameters.");
        userParametersPanel.getToolBar().add(editButton);
        editButton.addActionListener(e -> editUserParameters());
    }

    private void editUserParameters() {
        GraphNodeParameterReferenceGroupCollection copy = new GraphNodeParameterReferenceGroupCollection(getProject().getPipelineParameters().getExportedParameters());
        copy.setGraph(getProject().getGraph());
        JIPipeDesktopGraphNodeParameterReferenceGroupCollectionEditorUI graphNodeParameterReferenceGroupCollectionEditorUI = new JIPipeDesktopGraphNodeParameterReferenceGroupCollectionEditorUI(getDesktopWorkbench(),
                copy,
                MarkdownText.fromPluginResource("documentation/project-settings-parameters.md", new HashMap<>()),
                false);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setTitle("Edit user parameters");
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        dialog.setModal(true);
        JPanel contentPane = new JPanel(new BorderLayout());
        dialog.setContentPane(contentPane);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Save", UIUtils.getIconFromResources("actions/filesave.png"));
        confirmButton.addActionListener(e -> {
            getProject().getPipelineParameters().setExportedParameters(copy);
            dialog.setVisible(false);
            userParametersPanel.reloadForm();
        });
        buttonPanel.add(confirmButton);

        contentPane.add(graphNodeParameterReferenceGroupCollectionEditorUI, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(getDesktopProjectWorkbench().getWindow());
        dialog.setVisible(true);
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("description".equals(event.getKey())) {
            refreshDescription();
        } else if ("name".equals(event.getKey()) || "authors".equals(event.getKey())) {
            refreshHeaderText();
        } else {
            refreshTechnicalInfo();
            refreshHeaderButtons();
        }
    }

    private void initializeHeaderPanel() {
        headerPanel = new JIPipeDesktopImageFrameComponent(null, false, SizeFitMode.FitHeight, false);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 200));

        JIPipeDesktopFormPanel nameAndAuthorPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
        nameAndAuthorPanel.setLayout(new BoxLayout(nameAndAuthorPanel, BoxLayout.Y_AXIS));

        projectName = UIUtils.createReadonlyBorderlessTextField("Unnamed project");
        projectName.setOpaque(false);
        projectName.setFont(new Font(Font.DIALOG, Font.PLAIN, 40));
        projectName.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        nameAndAuthorPanel.addWideToForm(projectName, null);

        projectAuthors = new JPanel();
        projectAuthors.setLayout(new BoxLayout(projectAuthors, BoxLayout.X_AXIS));
        projectAuthors.setOpaque(false);
        projectAuthors.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        nameAndAuthorPanel.addWideToForm(projectAuthors, null);

        nameAndAuthorPanel.addVerticalGlue();
        headerPanel.add(nameAndAuthorPanel, BorderLayout.WEST);

        JIPipeDesktopFormPanel technicalInfo = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);

        licenseInfo = UIUtils.createReadonlyBorderlessTextField("No license");
        technicalInfo.addToForm(licenseInfo, new JLabel("Licensed under"), null);
        projectStats = UIUtils.createReadonlyBorderlessTextField("No information");
        technicalInfo.addToForm(projectStats, new JLabel("Project statistics"), null);
        technicalInfo.addVerticalGlue();

        headerPanel.add(technicalInfo, BorderLayout.EAST);

        initializeToolbar(headerPanel);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void initializeToolbar(JPanel topPanel) {
        JPanel toolBar = new JPanel();
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 32, 8, 0));
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setOpaque(false);

        openWebsiteButton = new JButton("Visit website", UIUtils.getIconFromResources("actions/web-browser.png"));
        openWebsiteButton.addActionListener(e -> UIUtils.openWebsite(getProject().getMetadata().getWebsite()));
        openWebsiteButton.setOpaque(false);
        openWebsiteButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openWebsiteButton);
        toolBar.add(Box.createHorizontalStrut(4));

        copyCitationButton = new JButton("Copy citation", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyCitationButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(getProject().getMetadata().getCitation());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });
        copyCitationButton.setOpaque(false);
        copyCitationButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(copyCitationButton);
        toolBar.add(Box.createHorizontalStrut(4));

        copyDependencyCitationsButton = new JButton("Copy cited sources", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyDependencyCitationsButton.addActionListener(e -> {
            StringBuilder stringBuilder = new StringBuilder();
            for (String dependencyCitation : getProject().getMetadata().getDependencyCitations()) {
                stringBuilder.append(dependencyCitation).append("\n\n");
            }
            StringSelection selection = new StringSelection(stringBuilder.toString());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });
        copyDependencyCitationsButton.setOpaque(false);
        copyDependencyCitationsButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(copyDependencyCitationsButton);
        toolBar.add(Box.createHorizontalStrut(4));

        showAcknowledgedAuthorsButton = new JButton("Show acknowledgements", UIUtils.getIconFromResources("actions/view-process-users.png"));
        showAcknowledgedAuthorsButton.addActionListener(e -> {
            JIPipeAuthorMetadata.openAuthorInfoWindow(this,
                    getProject().getMetadata().getAcknowledgements(),
                    getProject().getMetadata().getAcknowledgements().get(0));
        });
        showAcknowledgedAuthorsButton.setOpaque(false);
        showAcknowledgedAuthorsButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(showAcknowledgedAuthorsButton);
        toolBar.add(Box.createHorizontalStrut(4));

        toolBar.add(Box.createHorizontalGlue());

        JButton reportButton = new JButton("Generate report", UIUtils.getIconFromResources("actions/document-preview.png"));
        reportButton.setOpaque(false);
        reportButton.setBackground(new Color(0, 0, 0, 0));
        reportButton.setToolTipText("Opens a report that contains information about this project.");
        reportButton.addActionListener(e -> openProjectReport());
        toolBar.add(reportButton);

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.setOpaque(false);
        refreshButton.setBackground(new Color(0, 0, 0, 0));
        refreshButton.setToolTipText("Updates the contents of this page.");
        refreshButton.addActionListener(e -> refreshAll());
        toolBar.add(refreshButton);

        JButton openSettingsButton = new JButton("Application settings", UIUtils.getIconFromResources("actions/open-in-new-window.png"));
        openSettingsButton.setOpaque(false);
        openSettingsButton.setBackground(new Color(0, 0, 0, 0));
        openSettingsButton.setToolTipText("Opens the JIPipe application settings dialog");
        openSettingsButton.addActionListener(e -> getDesktopProjectWorkbench().openApplicationSettings(null));
        toolBar.add(openSettingsButton);

        JButton runProjectButton = new JButton("Run project", UIUtils.getIconFromResources("actions/play.png"));
        runProjectButton.setOpaque(false);
        runProjectButton.setBackground(new Color(0, 0, 0, 0));
        runProjectButton.setBorder(UIUtils.createButtonBorder(new Color(0x5CB85C)));
        runProjectButton.setToolTipText("Runs the whole project");
        runProjectButton.addActionListener(e -> getDesktopProjectWorkbench().runWholeProject());
        toolBar.add(runProjectButton);

        topPanel.add(toolBar, BorderLayout.SOUTH);
    }

    private void openProjectReport() {
        getDesktopProjectWorkbench().openProjectReport();
    }

}
