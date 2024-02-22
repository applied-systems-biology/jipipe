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

package org.hkijena.jipipe.ui.settings;

import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.bookmarks.BookmarkListPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ImageFrame;
import org.hkijena.jipipe.ui.components.html.ExtendedHTMLEditorKit;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameterreference.GraphNodeParameterReferenceGroupCollectionEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
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
public class JIPipeProjectOverviewUI extends JIPipeProjectWorkbenchPanel implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JTextPane descriptionReader;

    private final ParameterPanel projectSettingsParametersPanel;
    private final ParameterPanel userParametersPanel;
    private final ParameterPanel userDirectoriesPanel;
    private final JPanel runtimePartitionsPanel;
    private final JScrollPane descriptionReaderScrollPane;
    private JTextField licenseInfo;
    private JTextField projectName;
    private JTextField projectStats;
    private JPanel projectAuthors;
    private ImageFrame headerPanel;
    private JButton openWebsiteButton;
    private JButton copyCitationButton;
    private JButton copyDependencyCitationsButton;
    private JButton showAcknowledgedAuthorsButton;

    /**
     * Creates a new instance
     *
     * @param workbenchUI The workbench UI
     */
    public JIPipeProjectOverviewUI(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        descriptionReader = new JTextPane();
        descriptionReader.setContentType("text/html");
        descriptionReader.setEditorKit(new ExtendedHTMLEditorKit());
        descriptionReader.setEditable(false);
        UIUtils.registerHyperlinkHandler(descriptionReader);
        descriptionReaderScrollPane = new JScrollPane(descriptionReader);

        projectSettingsParametersPanel = new ParameterPanel(getWorkbench(),
                new JIPipeParameterTree(getProject().getMetadata()),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md", new HashMap<>()),
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.DOCUMENTATION_BELOW);
        userParametersPanel = new ParameterPanel(getWorkbench(),
                getProject().getPipelineParameters(),
                MarkdownDocument.fromPluginResource("documentation/project-info-parameters.md", new HashMap<>()),
                ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING
                        | ParameterPanel.NO_EMPTY_GROUP_HEADERS | ParameterPanel.WITH_DOCUMENTATION |
                        ParameterPanel.DOCUMENTATION_BELOW);
        userDirectoriesPanel = new ParameterPanel(getWorkbench(),
                getProject().getMetadata().getDirectories(),
                MarkdownDocument.fromPluginResource("documentation/project-info-directories.md", new HashMap<>()),
                ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING
                        | ParameterPanel.NO_EMPTY_GROUP_HEADERS | ParameterPanel.WITH_DOCUMENTATION |
                        ParameterPanel.DOCUMENTATION_BELOW);
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
                JIPipeAuthorMetadata.openAuthorInfoWindow(getWorkbench().getWindow(), getProject().getMetadata().getAuthors(), author);
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
        initializeUserDirectoriesPanel();
        initializeGeneralSettingsParametersPanel();
        initializeHeaderPanel();

        descriptionReaderScrollPane.setBorder(null);
        userParametersPanel.getScrollPane().setBorder(null);

        DocumentTabPane tabPane = new DocumentTabPane(true, DocumentTabPane.TabPlacement.Right);
        tabPane.addTab("Parameters",
                UIUtils.getIcon32FromResources("actions/configure.png"),
                userParametersPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        tabPane.addTab("Directories",
                UIUtils.getIcon32FromResources("actions/stock_folder-copy.png"),
                userDirectoriesPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        tabPane.addTab("Partitions",
                UIUtils.getIcon32FromResources("actions/runtime-partition.png"),
                runtimePartitionsPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        tabPane.addTab("Bookmarks",
                UIUtils.getIcon32FromResources("actions/bookmark.png"),
                new BookmarkListPanel(getWorkbench(), getProject().getGraph(), null, null),
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        tabPane.addTab("Settings",
                UIUtils.getIcon32FromResources("actions/wrench.png"),
                projectSettingsParametersPanel,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, descriptionReaderScrollPane, tabPane, new AutoResizeSplitPane.DynamicSidebarRatio(600, false));
        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeRuntimePartitionsPanel() {
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM,
                new JIPipeRuntimePartitionListEditor(getProjectWorkbench()),
                new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/project-info-runtime-partitions.md")),
                AutoResizeSplitPane.RATIO_3_TO_1);
        runtimePartitionsPanel.add(splitPane, BorderLayout.CENTER);
    }

    private void initializeGeneralSettingsParametersPanel() {
        projectSettingsParametersPanel.setCustomIsParameterCollectionVisible((tree, collection) -> {
            if(collection == getProject().getMetadata().getDirectories()) {
                return false;
            }
            return true;
        });
        projectSettingsParametersPanel.setCustomIsParameterVisible((tree, parameter) -> {
            if (parameter.getKey().contains("template"))
                return false;
            if (parameter.getKey().equals("thumbnail"))
                return false;
//            if (parameter.getKey().equals("update-site-dependencies"))
//                return false;
            return !parameter.isHidden();
        });
    }

    private void initializeUserDirectoriesPanel() {
        // Nothing yet
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
        GraphNodeParameterReferenceGroupCollectionEditorUI graphNodeParameterReferenceGroupCollectionEditorUI = new GraphNodeParameterReferenceGroupCollectionEditorUI(getWorkbench(),
                copy,
                MarkdownDocument.fromPluginResource("documentation/project-settings-parameters.md", new HashMap<>()),
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

        JButton confirmButton = new JButton("Save", UIUtils.getIconFromResources("actions/save.png"));
        confirmButton.addActionListener(e -> {
            getProject().getPipelineParameters().setExportedParameters(copy);
            dialog.setVisible(false);
            userParametersPanel.reloadForm();
        });
        buttonPanel.add(confirmButton);

        contentPane.add(graphNodeParameterReferenceGroupCollectionEditorUI, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setSize(1024,768);
        dialog.setLocationRelativeTo(getProjectWorkbench().getWindow());
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
        headerPanel = new ImageFrame(null, false, SizeFitMode.FitHeight, false);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 200));

        FormPanel nameAndAuthorPanel = new FormPanel(null, FormPanel.TRANSPARENT_BACKGROUND);
        nameAndAuthorPanel.setLayout(new BoxLayout(nameAndAuthorPanel, BoxLayout.Y_AXIS));

        projectName = UIUtils.makeReadonlyBorderlessTextField("Unnamed project");
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

        FormPanel technicalInfo = new FormPanel(null, FormPanel.TRANSPARENT_BACKGROUND);

        licenseInfo = UIUtils.makeReadonlyBorderlessTextField("No license");
        technicalInfo.addToForm(licenseInfo, new JLabel("Licensed under"), null);
        projectStats = UIUtils.makeReadonlyBorderlessTextField("No information");
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
        openSettingsButton.addActionListener(e -> getProjectWorkbench().openApplicationSettings(null));
        toolBar.add(openSettingsButton);

        topPanel.add(toolBar, BorderLayout.SOUTH);
    }

    private void openProjectReport() {
        getProjectWorkbench().openProjectReport();
    }

}
