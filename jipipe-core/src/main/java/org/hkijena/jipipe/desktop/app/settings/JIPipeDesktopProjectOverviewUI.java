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
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProjectDirectories;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.parameterreference.JIPipeDesktopGraphNodeParameterReferenceGroupCollectionEditorUI;
import org.hkijena.jipipe.desktop.app.settings.project.JIPipeDesktopMergedProjectSettings;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopImageFrameComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditorKit;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopDynamicParameterEditorDialog;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.SizeFitMode;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;

/**
 * UI that gives an overview of a pipeline (shows parameters, etc.)
 */
public class JIPipeDesktopProjectOverviewUI extends JIPipeDesktopProjectWorkbenchPanel {

    private final JTextPane descriptionReader;
    private final JScrollPane descriptionReaderScrollPane;

    private final JPanel runtimePartitionsPanel;
    private final JIPipeDesktopParameterFormPanel userParametersPanel;
    private final JIPipeDesktopRibbon userParametersRibbon = new JIPipeDesktopRibbon(2);

    private JTextField licenseInfo;
    private JTextField projectName;
    private JTextField projectStats;
    private JPanel projectAuthors;
    private JIPipeDesktopImageFrameComponent headerPanel;
    private JButton openWebsiteButton;
    private JButton copyCitationButton;
    private JButton copyDependencyCitationsButton;
    private JButton showAcknowledgedAuthorsButton;
    private final JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();

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

        userParametersPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                new JIPipeDesktopMergedProjectSettings(getProject()),
                MarkdownText.fromPluginResource("documentation/project-user-parameters.md"),
                JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_BELOW);
        runtimePartitionsPanel = new JPanel(new BorderLayout());

        initialize();
        refreshAll();
    }

    private void refreshAll() {
        renderBackgroundPanel();
        refreshDescription();
        refreshHeaderText();
        refreshTechnicalInfo();
        refreshHeaderButtons();
        refreshParameters();
    }

    private void refreshParameters() {
        userParametersPanel.reloadForm();
        if(userParametersPanel.getParameterTree().getParameters().isEmpty()) {
            userParametersPanel.clear();
            userParametersPanel.addWideToForm(UIUtils.createInfoLabel("This project has no parameters",
                    "Use the options above to add or link parameters into this panel."));
            userParametersPanel.addVerticalGlue();
        }
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
            openWebsiteButton.setVisible(true);
        } else {
            openWebsiteButton.setVisible(false);
        }
        if (!StringUtils.isNullOrEmpty(getProject().getMetadata().getCitation())) {
            copyCitationButton.setToolTipText(getProject().getMetadata().getCitation());
            copyCitationButton.setVisible(true);
        } else {
            copyCitationButton.setVisible(false);
        }
        if (getProject().getMetadata().getDependencyCitations().isEmpty()) {
            copyDependencyCitationsButton.setVisible(false);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (String dependencyCitation : getProject().getMetadata().getDependencyCitations()) {
                stringBuilder.append(dependencyCitation).append("\n\n");
            }
            copyDependencyCitationsButton.setToolTipText(stringBuilder.toString());
            copyDependencyCitationsButton.setVisible(true);
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

        JPanel userParametersContainer = new JPanel(new BorderLayout());
        userParametersContainer.add(userParametersRibbon, BorderLayout.NORTH);
        userParametersContainer.add(userParametersPanel, BorderLayout.CENTER);

        dockPanel.addDockPanel("PARAMETERS",
                "Parameters",
                UIUtils.getIcon32FromResources("actions/configure.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                true,
                0,
                userParametersContainer);
        dockPanel.addDockPanel("PARTITIONS",
                "Partitions",
                UIUtils.getIcon32FromResources("actions/runtime-partition.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                0,
                runtimePartitionsPanel);
        dockPanel.addDockPanel("BOOKMARKS",
                "Bookmarks",
                UIUtils.getIcon32FromResources("actions/bookmark.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                0,
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getProject().getGraph(), null, null));

        dockPanel.setBackgroundComponent(descriptionReaderScrollPane);
        add(dockPanel, BorderLayout.CENTER);
    }

    private void initializeRuntimePartitionsPanel() {
        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.TOP_BOTTOM,
                new JIPipeDesktopRuntimePartitionListEditor(getDesktopProjectWorkbench()),
                new JIPipeDesktopMarkdownReader(false, MarkdownText.fromPluginResource("documentation/project-info-runtime-partitions.md")),
                JIPipeDesktopSplitPane.RATIO_3_TO_1);
        runtimePartitionsPanel.add(splitPane, BorderLayout.CENTER);
    }

    private void initializeUserParametersPanel() {
        JIPipeDesktopRibbon.Task parametersTask = userParametersRibbon.getOrCreateTask("Parameters");
        JIPipeDesktopRibbon.Band modifyParametersBand = parametersTask.getOrCreateBand("Modify");
        modifyParametersBand.addLargeButton("Global", "Add/edit custom global parameters", UIUtils.getIcon32FromResources("actions/configure3.png"), this::editGlobalParameters);
        modifyParametersBand.addLargeButton("References", "Reference parameters from the pipeline", UIUtils.getIcon32FromResources("actions/edit-link.png"), this::editReferencedParameters);
        modifyParametersBand.addLargeMenuButton("Directories", "Modify global directories", UIUtils.getIcon32FromResources("actions/document-open-folder.png"),
                UIUtils.createMenuItem("Add new directory ...", "Adds an existing path/directory as new entry into the directory list", UIUtils.getIconFromResources("actions/add.png"), this::addDirectoryParameter),
                UIUtils.createMenuItem("Configure ...", "Opens the relevant page in the project settings", UIUtils.getIconFromResources("actions/configure.png"), this::editDirectoryParameters));
        JIPipeDesktopRibbon.Band viewBand = parametersTask.getOrCreateBand("View");
        viewBand.addLargeButton("Refresh", "Refreshes the parameters", UIUtils.getIcon32FromResources("actions/stock_refresh.png"), this::refreshAll);
        userParametersRibbon.rebuildRibbon();
    }

    private void addDirectoryParameter() {
        Path path = JIPipeFileChooserApplicationSettings.openPath(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Add new project-wide path/directory");
        if(path != null) {
            JIPipeProjectDirectories.DirectoryEntry entry = new JIPipeProjectDirectories.DirectoryEntry();
            entry.setPath(path);
            entry.setName(path.getFileName().toString());
            while(true) {
                if(JIPipeDesktopParameterFormPanel.showDialog(getDesktopProjectWorkbench(), entry, new MarkdownText("# Add new project-wide path/directory\n\n" +
                        "Please provide at least a unique key that identifies the path and allows to recall it from within the workflow. " +
                        "You can also request that the path must exist."), "Add new project-wide path/directory", JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS)) {
                    if(StringUtils.isNullOrEmpty(entry.getKey())) {
                        JOptionPane.showMessageDialog(this, "Please provide a key", "Add new project-wide path/directory", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                    if(getProject().getMetadata().getDirectories().getDirectoriesAsInstance().stream().anyMatch(e -> Objects.equals(e.getKey(), entry.getKey()))) {
                        JOptionPane.showMessageDialog(this, "The key already exists", "Add new project-wide path/directory", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                    getProject().getMetadata().getDirectories().getDirectories().addFromTemplate(entry);
                    refreshParameters();
                    break;
                }
            }
        }
    }

    private void editDirectoryParameters() {
        getDesktopProjectWorkbench().openProjectSettings("/General/Project-wide directories");
        refreshParameters();
    }

    private void editGlobalParameters() {
        JIPipeDesktopDynamicParameterEditorDialog dialog = new JIPipeDesktopDynamicParameterEditorDialog(SwingUtilities.getWindowAncestor(this),
                getDesktopProjectWorkbench(),
               getProject().getMetadata().getGlobalParameters());
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setModal(true);
        dialog.setVisible(true);
        refreshParameters();
    }

    private void editReferencedParameters() {
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
            refreshParameters();
        });
        buttonPanel.add(confirmButton);

        contentPane.add(graphNodeParameterReferenceGroupCollectionEditorUI, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(getDesktopProjectWorkbench().getWindow());
        dialog.setVisible(true);
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
        nameAndAuthorPanel.addWideToForm(UIUtils.boxHorizontal(projectName,
                UIUtils.makeButtonTransparent(UIUtils.createButton("", UIUtils.getIcon32FromResources("actions/edit.png"), this::editProjectMetadata))), null);

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

    private void editProjectMetadata() {
        getDesktopProjectWorkbench().openProjectSettings("/General/Project metadata");
        refreshAll();
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

        JButton openProjectSettingsButton = new JButton("Project settings", UIUtils.getIconFromResources("actions/configure.png"));
        openProjectSettingsButton.setOpaque(false);
        openProjectSettingsButton.setBackground(new Color(0, 0, 0, 0));
        openProjectSettingsButton.setToolTipText("Opens the project settings dialog");
        openProjectSettingsButton.addActionListener(e -> {
            getDesktopProjectWorkbench().openProjectSettings(null);
            refreshAll();
        });
        toolBar.add(openProjectSettingsButton);

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
