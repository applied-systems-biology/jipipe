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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProjectDirectories;
import org.hkijena.jipipe.api.project.JIPipeProjectRunSetsConfiguration;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.api.run.JIPipeProjectRunSet;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopMultiAlgorithmCacheBrowserUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorLogPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.properties.JIPipeDesktopGraphEditorErrorPanel;
import org.hkijena.jipipe.desktop.app.parameterreference.JIPipeDesktopGraphNodeParameterReferenceGroupCollectionEditorUI;
import org.hkijena.jipipe.desktop.app.settings.project.JIPipeDesktopMergedProjectSettings;
import org.hkijena.jipipe.desktop.app.settings.project.JIPipeDesktopProjectOverviewRunManager;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormHelpPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopImageFrameComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditor;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditorKit;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopDynamicParameterEditorDialog;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_HELP;

/**
 * UI that gives an overview of a pipeline (shows parameters, etc.)
 */
public class JIPipeDesktopProjectOverviewUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeProjectRunSetsConfiguration.RunSetsModifiedEventListener {

    private final JIPipeDesktopFormPanel centerPanel;
    private final JTextPane descriptionReader;

    private final JPanel runtimePartitionsPanel;
    private final JIPipeDesktopRunSetsListEditor runSetsEditor;
    private final JIPipeDesktopParameterFormPanel userParametersPanel;
    private final JIPipeDesktopMultiAlgorithmCacheBrowserUI resultsPanel;
    private final JIPipeDesktopRibbon userParametersRibbon = new JIPipeDesktopRibbon(2);
    private final JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();
    private JTextField licenseInfo;
    private JTextField projectName;
    private JTextField projectStats;
    private JPanel projectAuthors;
    private JIPipeDesktopImageFrameComponent headerPanel;
    private JButton openWebsiteButton;
    private JButton copyCitationButton;
    private JButton copyDependencyCitationsButton;
    private JButton showAcknowledgedAuthorsButton;

    /**
     * Creates a new instance
     *
     * @param workbench The workbench UI
     */
    public JIPipeDesktopProjectOverviewUI(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);

        // Run sets
        runSetsEditor = new JIPipeDesktopRunSetsListEditor(workbench);
        workbench.getProject().getRunSetsConfiguration().getModifiedEventEmitter().subscribe(this);

        // Description
        descriptionReader = new JTextPane();
        descriptionReader.setContentType("text/html");
        descriptionReader.setEditorKit(new JIPipeDesktopHTMLEditorKit());
        descriptionReader.setEditable(false);
        UIUtils.registerHyperlinkHandler(descriptionReader);

        // Center panel
        centerPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        resultsPanel = new JIPipeDesktopMultiAlgorithmCacheBrowserUI(getDesktopProjectWorkbench());
        userParametersPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                new JIPipeDesktopMergedProjectSettings(getProject()),
                MarkdownText.fromPluginResource("documentation/project-user-parameters.md"),
                JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_EXTERNAL);
        runtimePartitionsPanel = new JPanel(new BorderLayout());

        initialize();
        refreshAll();

        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribe(this);
    }

    public JIPipeDesktopDockPanel getDockPanel() {
        return dockPanel;
    }

    private void refreshAll() {
        renderBackgroundPanel();
        refreshCenterPanel();
        refreshHeaderText();
        refreshTechnicalInfo();
        refreshHeaderButtons();
        refreshParameters();
    }

    private void refreshParameters() {
        userParametersPanel.reloadForm();
        if (userParametersPanel.getParameterTree().getParameters().isEmpty()) {
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

    private void refreshCenterPanel() {
        centerPanel.clear();

        JPanel tipsPanel = new JPanel();
        tipsPanel.setLayout(new BoxLayout(tipsPanel, BoxLayout.X_AXIS));

        if (!StringUtils.isNullOrEmpty(getProject().getMetadata().getDescription().toPlainText())) {
            addPanelToCenterPanel(UIUtils.getIcon32FromResources("status/messagebox_info.png"), "Description", descriptionReader, UIUtils.makeButtonTransparent(UIUtils.createButton("", UIUtils.getIconFromResources("actions/edit.png"), this::editProjectDescription)));
            descriptionReader.setText(getProject().getMetadata().getDescription().getHtml());
        } else {
            addToTipsPanel(tipsPanel, "Write a description", "Write a workflow description to help people who are unfamiliar with your pipeline.",
                    UIUtils.makeButtonTransparent(UIUtils.createButton("Edit metadata", UIUtils.getIconFromResources("actions/edit.png"), this::editProjectDescription)));
        }

        if (StringUtils.isNullOrEmpty(getProject().getMetadata().getLicense())) {
            createLicenseTip(tipsPanel);
        }

        createRunSetsPanel();
        createRunCompartmentsPanel();

        createCompartmentsTipIfNeeded(tipsPanel);
        createCompartmentsOutputTipIfNeeded(tipsPanel);
        createArtifactUpgradeTipsIfNeeded(tipsPanel);

        // Handle the tips panel
        if (tipsPanel.getComponentCount() > 0) {
            tipsPanel.add(Box.createHorizontalGlue());
            JScrollPane scrollPane = new JScrollPane(tipsPanel);
            scrollPane.setMinimumSize(new Dimension(300, 300));
            scrollPane.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            centerPanel.addWideToForm(scrollPane);
        }
        centerPanel.addVerticalGlue();

    }

    private void editProjectDescription() {
        JIPipeDesktopHTMLEditor editor = new JIPipeDesktopHTMLEditor(getDesktopProjectWorkbench(), JIPipeDesktopHTMLEditor.Mode.Full, JIPipeDesktopHTMLEditor.WITH_SCROLL_BAR);
        editor.setText(getProject().getMetadata().getDescription().getHtml());
        if (UIUtils.showConfirmDialog(this, "Edit project description", new Dimension(800, 600), editor)) {
            getProject().getMetadata().setDescription(new HTMLText(editor.getHTML()));
            refreshCenterPanel();
        }
    }

    private void createArtifactUpgradeTipsIfNeeded(JPanel tipsPanel) {
        List<JIPipeArtifactEnvironment> environments = new ArrayList<>();
        for (JIPipeProjectSettingsSheet settingsSheet : getProject().getSettingsSheets().values()) {
            JIPipeParameterTree parameterTree = new JIPipeParameterTree(settingsSheet);
            for (JIPipeParameterAccess access : parameterTree.getParameters().values()) {
                Object o = access.get(Object.class);
                if (o instanceof OptionalParameter) {
                    if (((OptionalParameter<?>) o).isEnabled() && ((OptionalParameter<?>) o).getContent() instanceof JIPipeArtifactEnvironment) {
                        environments.add((JIPipeArtifactEnvironment) ((OptionalParameter<?>) o).getContent());
                    }
                } else if (o instanceof JIPipeArtifactEnvironment) {
                    environments.add((JIPipeArtifactEnvironment) o);
                }
            }
        }
        List<ArtifactUpgrade> upgrades = new ArrayList<>();
        for (JIPipeArtifactEnvironment environment : environments) {
            if (environment.isLoadFromArtifact() && !StringUtils.isNullOrEmpty(environment.getArtifactQuery().getQuery()) && environment.getArtifactQuery().isStatic()) {
                try {
                    JIPipeArtifact current = JIPipe.getArtifacts().queryCachedArtifact(environment.getArtifactQuery().getQuery());
                    List<JIPipeArtifact> candidates = JIPipe.getArtifacts().queryCachedArtifacts(environment.getArtifactQuery().getBaseQuery());
                    List<JIPipeArtifact> revisionUpgrades = new ArrayList<>();
                    List<JIPipeArtifact> accelerationUpgrades = new ArrayList<>();
                    if (current != null) {
                        int revisionVersion = current.getVersionRevision();
                        String baseVersion = current.getVersionWithoutRevision();

                        for (JIPipeArtifact candidate : candidates) {
                            if (candidate.isCompatible()) {
                                String candidateBaseVersion = candidate.getVersionWithoutRevision();
                                int candidateRevision = candidate.getVersionRevision();
                                if (StringUtils.compareVersions(candidateBaseVersion, baseVersion) == 0) {
//                                    System.out.println("Found upgrade from " + current.getFullId() + " to " + candidate.getFullId());
                                    if (candidateRevision > revisionVersion && !candidate.getFullId().equals(current.getFullId())) {
                                        revisionUpgrades.add(candidate);
                                    }
                                    if (candidateRevision >= revisionVersion) {
                                        accelerationUpgrades.add(candidate);
                                    }
                                }
                            }
                        }
                    }

                    if (!accelerationUpgrades.isEmpty()) {
                        accelerationUpgrades = Collections.singletonList(JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(accelerationUpgrades));
                        if (accelerationUpgrades.get(0).getFullId().equals(current.getFullId())) {
                            accelerationUpgrades = Collections.emptyList();
                        }
                    }

                    if (!accelerationUpgrades.isEmpty() || !revisionUpgrades.isEmpty()) {
                        upgrades.add(new ArtifactUpgrade(environment, current, revisionUpgrades, accelerationUpgrades));
                    }

                } catch (Throwable ignored) {

                }
            }
        }

        if (!upgrades.isEmpty()) {
            addToTipsPanel(tipsPanel, "Update third-party artifacts", "Your project has some non-optimal configurations regarding " + StringUtils.formatPluralS(upgrades.size(), "third-party artifact") +
                            ". Alternatives were detected that may allow to utilize your hardware better or contain important bugfixes.",
                    UIUtils.makeButtonTransparent(UIUtils.createButton("Review updates", UIUtils.getIconFromResources("actions/list-check.png"), () -> upgradeArtifacts(upgrades))));
        }
    }

    private void upgradeArtifacts(List<ArtifactUpgrade> upgrades) {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        List<JComboBox<String>> comboBoxes = new ArrayList<>();

        formPanel.addToForm(UIUtils.createJLabel("New version", 16), UIUtils.createJLabel("Old version", 16));
        formPanel.addWideToForm(new JSeparator(JSeparator.HORIZONTAL));

        for (ArtifactUpgrade artifactUpgrade : upgrades) {
            JComboBox<String> comboBox = new JComboBox<>();
            for (JIPipeArtifact revisionUpgrade : artifactUpgrade.revisionUpgrades) {
                comboBox.addItem(revisionUpgrade.getFullId());
            }
            for (JIPipeArtifact accelerationUpgrade : artifactUpgrade.accelerationUpgrades) {
                comboBox.addItem(accelerationUpgrade.getFullId());
            }
            comboBox.addItem("Keep as-is");
            comboBoxes.add(comboBox);

            formPanel.addToForm(comboBox, new JLabel(artifactUpgrade.current.getFullId(), UIUtils.getIconFromResources("actions/run-build-install.png"), JLabel.LEFT));
        }
        int numSuccesses = 0;
        if (JIPipeDesktopFormPanel.showDialog(this, formPanel, "Update third-party artifacts")) {
            for (int i = 0; i < upgrades.size(); i++) {
                ArtifactUpgrade upgrade = upgrades.get(i);
                JComboBox<String> comboBox = comboBoxes.get(i);
                String selectedItem = StringUtils.nullToEmpty(comboBox.getSelectedItem());

                if (!StringUtils.isNullOrEmpty(selectedItem) && !"Keep as-is".equals(selectedItem)) {
                    upgrade.environment.setArtifactQuery(new JIPipeArtifactQueryParameter(selectedItem));
                    ++numSuccesses;
                }
            }

            if (numSuccesses > 0) {
                JOptionPane.showMessageDialog(this, StringUtils.wrapHtml(StringUtils.formatPluralS(numSuccesses, "artifact") + " were updated.<br/>JIPipe will automatically take care of downloading and setting up the artifacts."));
                refreshCenterPanel();
            }


        }
    }

    @Override
    public void onRunSetsModified(JIPipeProjectRunSetsConfiguration.RunSetsModifiedEvent event) {
        refreshCenterPanel();
    }

    private void createRunSetsPanel() {
        if (!getProject().getRunSetsConfiguration().getRunSets().isEmpty()) {
            JPanel listPanel = UIUtils.boxVertical();
            for (JIPipeProjectRunSet runSet : getProject().getRunSetsConfiguration().getRunSets()) {
                JPanel outputPanel = new JPanel(new BorderLayout(16, 0));
                outputPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(4, 4, 4, 4),
                        new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
                ));
                Color color = null;
                if (runSet.getColor().isEnabled()) {
                    color = runSet.getColor().getContent();
                }
                outputPanel.add(new JLabel(new SolidColorIcon(8, 32, color != null ? color : UIManager.getColor("Panel.background"), UIUtils.getControlBorderColor())), BorderLayout.WEST);
                outputPanel.add(new JLabel(runSet.getDisplayName(), UIUtils.getIconFromResources("actions/debug-run.png"), JLabel.LEFT), BorderLayout.CENTER);

                JButton runButton = new JButton("Run", UIUtils.getIconFromResources("actions/run-play.png"));
                JPopupMenu runMenu = UIUtils.addPopupMenuToButton(runButton);
                runMenu.add(UIUtils.createMenuItem("Update cache (default)", "Runs the output and stores the results in the memory cache", UIUtils.getIcon16FromResources("actions/update-cache.png"), () -> {
                    doUpdateCache(runSet, false);
                }));
                runMenu.add(UIUtils.createMenuItem("Cache intermediate results", "Runs the output and stores the results and intermediate" +
                        " results in the memory cache (memory-intensive for large workflows!)", UIUtils.getIcon16FromResources("actions/cache-intermediate-results.png"), () -> {
                    doUpdateCache(runSet, true);
                }));
                runMenu.addSeparator();
                runMenu.add(UIUtils.createMenuItem("Run (no cache)", "Runs the output without caching any results in memory", UIUtils.getIcon16FromResources("actions/play.png"), () -> {
                    doRun(runSet);
                }));

                outputPanel.add(UIUtils.boxHorizontal(
                        UIUtils.createButton("Help", UIUtils.getIconFromResources("actions/help.png"), () -> {
                            dockPanel.activatePanel(DOCK_NODE_CONTEXT_HELP, true);
                            JIPipeDesktopFormHelpPanel helpPanel = dockPanel.getPanelComponent(DOCK_NODE_CONTEXT_HELP, JIPipeDesktopFormHelpPanel.class);
                            if (!StringUtils.isNullOrEmpty(runSet.getDescription().toPlainText())) {
                                helpPanel.showContent(new MarkdownText("# " + runSet.getDisplayName() + "\n\n" + runSet.getDescription().getBody()));
                            } else {
                                helpPanel.showContent(new MarkdownText("# " + runSet.getDisplayName() + "\n\n*No description provided*"));
                            }
                        }),
                        UIUtils.createButton("Show results", UIUtils.getIconFromResources("actions/update-cache.png"), () -> {
                            doShowResults(runSet);
                        }),
                        runButton
                ), BorderLayout.EAST);

                listPanel.add(outputPanel);
            }
            addPanelToCenterPanel(UIUtils.getIcon32FromResources("actions/run-play.png"), "Run predefined node sets", listPanel);
        }
    }

    private void doRun(JIPipeProjectRunSet runSet) {
        List<JIPipeAlgorithm> nodes = JIPipeUtils.filterAlgorithmsList(runSet.resolveNodes(getProject()));
        if (nodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Unable to run the set '" + runSet.getDisplayName() + "'. No nodes found.", "Run set", JOptionPane.ERROR_MESSAGE);
            return;
        }
        doRun(nodes);
    }

    private void doShowResults(JIPipeProjectRunSet runSet) {
        List<JIPipeAlgorithm> nodes = JIPipeUtils.filterAlgorithmsList(runSet.resolveNodes(getProject()));
        if (nodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Unable to run the set '" + runSet.getDisplayName() + "'. No nodes found.", "Run set", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<JIPipeAlgorithm> nodesToDo = new ArrayList<>();
        for (JIPipeAlgorithm node : nodes) {
            Map<String, JIPipeDataTable> cachedData = getProject().getCache().query(node, node.getUUIDInParentGraph(), JIPipeProgressInfo.SILENT);
            if (cachedData == null || cachedData.isEmpty()) {
                nodesToDo.add(node);
            }
        }

        if (nodesToDo.isEmpty()) {
            showResults(nodes);
        } else {
            JComboBox<String> options = new JComboBox<>(new String[]{"Update cache (default)", "Cache intermediate results"});
            JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
            formPanel.addWideToForm(new JLabel("<html>" +
                    "There are missing cached results.<br/>" +
                    "Do you want to run the pipeline to generate them?" +
                    "</html>"));
            formPanel.addToForm(options, new JLabel("Operation"));

            if (JOptionPane.showConfirmDialog(this,
                    formPanel,
                    "Show results",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                doUpdateCache(nodesToDo, Objects.equals(options.getSelectedItem(), "Cache intermediate results"));
            }
        }

    }

    private void doUpdateCache(JIPipeProjectRunSet runSet, boolean intermediateResults) {
        List<JIPipeAlgorithm> nodes = JIPipeUtils.filterAlgorithmsList(runSet.resolveNodes(getProject()));
        if (nodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Unable to run the set '" + runSet.getDisplayName() + "'. No nodes found.", "Run set", JOptionPane.ERROR_MESSAGE);
            return;
        }
        doUpdateCache(nodes, intermediateResults);
    }

    private void createRunCompartmentsPanel() {
        if (!getProject().getMetadata().isShowCompartmentsRunPanelInOverview()) {
            return;
        }
        List<JIPipeProjectCompartmentOutput> outputList = new ArrayList<>();
        for (JIPipeGraphNode graphNode : getProject().getCompartmentGraph().traverse()) {
            if (graphNode instanceof JIPipeProjectCompartment) {
                if (((JIPipeProjectCompartment) graphNode).isShowInProjectOverview()) {
                    for (JIPipeProjectCompartmentOutput output : ((JIPipeProjectCompartment) graphNode).getOutputNodes().values()) {
                        if (output.isShowInProjectOverview()) {
                            outputList.add(output);
                        }
                    }
                }
            }
        }
        if (!outputList.isEmpty()) {
            JPanel listPanel = UIUtils.boxVertical();

            for (JIPipeProjectCompartmentOutput output : outputList) {
                JPanel outputPanel = new JPanel(new BorderLayout(16, 0));
                outputPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(4, 4, 4, 4),
                        new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
                ));
                Color color = null;
                if (output.getProjectCompartment().getProjectOverviewColor().isEnabled()) {
                    color = output.getProjectCompartment().getProjectOverviewColor().getContent();
                }
                if (output.getProjectOverviewColor().isEnabled()) {
                    color = output.getProjectOverviewColor().getContent();
                }
                outputPanel.add(new JLabel(new SolidColorIcon(8, 32, color != null ? color : UIManager.getColor("Panel.background"), UIUtils.getControlBorderColor())), BorderLayout.WEST);
                outputPanel.add(new JLabel(output.getDisplayName(), UIUtils.getIconFromResources("actions/graph-compartment.png"), JLabel.LEFT), BorderLayout.CENTER);

                JButton runButton = new JButton("Run", UIUtils.getIconFromResources("actions/run-play.png"));
                JPopupMenu runMenu = UIUtils.addPopupMenuToButton(runButton);
                runMenu.add(UIUtils.createMenuItem("Update cache (default)", "Runs the output and stores the results in the memory cache", UIUtils.getIcon16FromResources("actions/update-cache.png"), () -> {
                    doUpdateCache(Collections.singletonList(output), false);
                }));
                runMenu.add(UIUtils.createMenuItem("Cache intermediate results", "Runs the output and stores the results and intermediate" +
                        " results in the memory cache (memory-intensive for large workflows!)", UIUtils.getIcon16FromResources("actions/cache-intermediate-results.png"), () -> {
                    doUpdateCache(Collections.singletonList(output), true);
                }));
                runMenu.addSeparator();
                runMenu.add(UIUtils.createMenuItem("Run (no cache)", "Runs the output without caching any results in memory", UIUtils.getIcon16FromResources("actions/play.png"), () -> {
                    doRun(Collections.singletonList(output));
                }));

                outputPanel.add(UIUtils.boxHorizontal(
                        UIUtils.createButton("Go to", UIUtils.getIconFromResources("actions/go-jump.png"), () -> {
                            getDesktopProjectWorkbench().getOrOpenPipelineEditorTab(output.getProjectCompartment(), true);
                        }),
                        Box.createHorizontalStrut(16),
                        UIUtils.createButton("Help", UIUtils.getIconFromResources("actions/help.png"), () -> {
                            dockPanel.activatePanel(DOCK_NODE_CONTEXT_HELP, true);
                            JIPipeDesktopFormHelpPanel helpPanel = dockPanel.getPanelComponent(DOCK_NODE_CONTEXT_HELP, JIPipeDesktopFormHelpPanel.class);
                            if (!StringUtils.isNullOrEmpty(output.getProjectCompartment().getCustomDescription().toPlainText().trim()) || !StringUtils.isNullOrEmpty(output.getCustomDescription().toPlainText().trim())) {
                                helpPanel.showContent(new MarkdownText("# " + output.getDisplayName() + "\n\n" + output.getProjectCompartment().getCustomDescription().getBody() + "\n\n" + output.getCustomDescription().getBody()));
                            } else {
                                helpPanel.showContent(new MarkdownText("# " + output.getDisplayName() + "\n\n*No description provided*"));
                            }

                        }),
                        UIUtils.createButton("Show results", UIUtils.getIconFromResources("actions/update-cache.png"), () -> {
                            doShowResults(output);
                        }),
                        runButton
                ), BorderLayout.EAST);

                listPanel.add(outputPanel);
            }

            addPanelToCenterPanel(UIUtils.getIcon32FromResources("actions/run-play.png"), "Run compartment", listPanel);
        }
    }

    private void doRun(List<JIPipeAlgorithm> nodes) {
        JIPipeDesktopProjectOverviewRunManager runManager = new JIPipeDesktopProjectOverviewRunManager(getProject(),
                dockPanel,
                this,
                nodes,
                true);
        runManager.run(false, false, false, false);
    }

    private void doShowResults(JIPipeProjectCompartmentOutput output) {
        Map<String, JIPipeDataTable> cachedData = getProject().getCache().query(output, output.getUUIDInParentGraph(), JIPipeProgressInfo.SILENT);
        if (cachedData == null || cachedData.isEmpty()) {

            JComboBox<String> options = new JComboBox<>(new String[]{"Update cache (default)", "Cache intermediate results"});
            JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
            formPanel.addWideToForm(new JLabel("<html>" +
                    "The output " + output.getDisplayName() + " currently has no cached results.<br/>" +
                    "Do you want to run the pipeline to generate them?" +
                    "</html>"));
            formPanel.addToForm(options, new JLabel("Operation"));

            if (JOptionPane.showConfirmDialog(this,
                    formPanel,
                    "Show results",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                doUpdateCache(Collections.singletonList(output), Objects.equals(options.getSelectedItem(), "Cache intermediate results"));
            }
        } else {
            showResults(Collections.singletonList(output));
        }
    }

    public void showResults(List<JIPipeAlgorithm> nodes) {
        resultsPanel.setDisplayedAlgorithms(nodes);
        dockPanel.activatePanel("RESULTS", true);
    }

    private void doUpdateCache(List<JIPipeAlgorithm> output, boolean intermediateResults) {
        JIPipeDesktopProjectOverviewRunManager runManager = new JIPipeDesktopProjectOverviewRunManager(getProject(),
                dockPanel,
                this,
                output,
                true);
        runManager.run(true, false, intermediateResults, false);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {

    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {

    }

    private void createCompartmentsOutputTipIfNeeded(JPanel tipsPanel) {
        int numHits = 0;
        for (JIPipeProjectCompartment compartment : getProject().getCompartments().values()) {
            if (compartment.getOutputNodes().isEmpty()) {
                numHits++;
            }
        }
        if (numHits > 0) {
            addToTipsPanel(tipsPanel, "Let compartments output data", "If you setup at least one output for each compartment, you can pass data to other compartments. " +
                            "Additionally, you will be able to run the outputs from the Compartments view and from here.",
                    UIUtils.makeButtonTransparent(UIUtils.createButton("Show compartments", UIUtils.getIconFromResources("actions/graph-compartments.png"), this::openCompartmentsEditor)));
        }
    }

    private void createCompartmentsTipIfNeeded(JPanel tipsPanel) {
        int nodeCount = getProject().getGraph().getNodeCount();
        int compartmentCount = getProject().getCompartments().size();

        if (nodeCount > 30 && compartmentCount == 1) {
            addToTipsPanel(tipsPanel, "Consider organizing your project", "Use compartments to split your pipeline into smaller units, so it is easier " +
                            "to navigate through your project.",
                    UIUtils.makeButtonTransparent(UIUtils.createButton("Show compartments", UIUtils.getIconFromResources("actions/graph-compartments.png"), this::openCompartmentsEditor)));
        }
    }

    private void openCompartmentsEditor() {
        getDesktopProjectWorkbench().getDocumentTabPane().selectSingletonTab(JIPipeDesktopProjectWorkbench.TAB_COMPARTMENT_EDITOR);
    }

    private void createLicenseTip(JPanel tipsPanel) {
        JButton button = UIUtils.makeButtonTransparent(UIUtils.createButton("Choose a license", UIUtils.getIconFromResources("actions/edit.png"), () -> {
        }));
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(button);
        for (String license : Arrays.asList(
                "CC-BY-4.0",
                "MIT",
                "Apache-2.0",
                "GPL-3.0",
                "GPL-2.0",
                "LGPL-3.0",
                "LGPL-2.1",
                "AGPL-3.0",
                "BSD-2-Clause",
                "BSD-3-Clause",
                "MPL-2.0",
                "EPL-2.0",
                "Unlicense"
        )) {
            popupMenu.add(UIUtils.createMenuItem(license, "Set the license to " + license, UIUtils.getIconFromResources("actions/copyright.png"), () -> {
                getProject().getMetadata().setLicense(license);
                refreshAll();
            }));
        }

        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Learn more ...", "Open https://choosealicense.com/", UIUtils.getIconFromResources("actions/web-browser.png"), () -> {
            UIUtils.openWebsite("https://choosealicense.com/");
        }));

        addToTipsPanel(tipsPanel, "Make your project reusable", "Set the license of your project (preferably to CC-BY-4.0), " +
                "so others can reuse it.", button);
    }

    private void addToTipsPanel(JPanel tipsPanel, String title, String text, Component... ctaComponents) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 16),
                new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setIcon(UIUtils.getIcon32FromResources("status/starred.png"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(UIUtils.createReadonlyBorderlessTextArea(text), BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.getControlBorderColor()));
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());
        for (Component component : ctaComponents) {
            toolBar.add(component);
        }
        panel.add(toolBar, BorderLayout.SOUTH);


        panel.setPreferredSize(new Dimension(300, 250));
        panel.setMaximumSize(new Dimension(300, 300));

        tipsPanel.add(panel);
    }

    private void addPanelToCenterPanel(Icon icon, String title, Component center, Component... titleBarComponents) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(16, 16, 16, 16),
                new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
        ));

        JLabel titleLabel = new JLabel(title, icon, JLabel.LEFT);
        titleLabel.setBorder(UIUtils.createEmptyBorder(8));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(titleLabel);
        toolBar.add(Box.createHorizontalGlue());
        for (Component titleBarComponent : titleBarComponents) {
            toolBar.add(titleBarComponent);
        }
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.getControlBorderColor()));
        toolBar.setBackground(ColorUtils.mix(JIPipeDesktopModernMetalTheme.PRIMARY5, ColorUtils.scaleHSV(UIManager.getColor("Panel.background"), 1, 1, 0.98f), 0.92));

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);

        centerPanel.addWideToForm(panel);

    }

    private void initialize() {
        setLayout(new BorderLayout());

        initializeRuntimePartitionsPanel();
        initializeUserParametersPanel();
        initializeHeaderPanel();

        userParametersPanel.getScrollPane().setBorder(null);

        JPanel userParametersContainer = new JPanel(new BorderLayout());
        userParametersContainer.add(userParametersRibbon, BorderLayout.NORTH);
        userParametersContainer.add(userParametersPanel, BorderLayout.CENTER);

        dockPanel.addDockPanel("PARAMETERS",
                "Parameters",
                UIUtils.getIcon32FromResources("actions/configure3.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                true,
                0,
                userParametersContainer);
        dockPanel.addDockPanel("RESULTS", "Results", UIUtils.getIcon32FromResources("actions/network-server-database.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                false,
                0,
                resultsPanel);
        dockPanel.addDockPanel("PARTITIONS",
                "Partitions",
                UIUtils.getIcon32FromResources("actions/runtime-partition.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                0,
                runtimePartitionsPanel);
        dockPanel.addDockPanel("RUN_SETS",
                "Run sets",
                UIUtils.getIcon32FromResources("actions/debug-run.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                0,
                runSetsEditor);
        dockPanel.addDockPanel("BOOKMARKS",
                "Bookmarks",
                UIUtils.getIcon32FromResources("actions/bookmark.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomLeft,
                false,
                0,
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getProject().getGraph(), null, null));
        dockPanel.addDockPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_LOG,
                "Log",
                UIUtils.getIcon32FromResources("actions/rabbitvcs-show_log.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                false,
                0, new JIPipeDesktopGraphEditorLogPanel(getDesktopWorkbench()));
        dockPanel.addDockPanel(AbstractJIPipeDesktopGraphEditorUI.DOCK_ERRORS,
                "Errors",
                UIUtils.getIcon32FromResources("actions/dialog-warning-2.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                false,
                0, new JIPipeDesktopGraphEditorErrorPanel(getDesktopWorkbench(), null));
        dockPanel.addDockPanel(DOCK_NODE_CONTEXT_HELP,
                "Documentation",
                UIUtils.getIcon32FromResources("actions/help-question.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                true,
                0, userParametersPanel.getHelpPanel());
        userParametersPanel.getContextHelpEventEmitter().subscribeLambda((source, event) -> {
            dockPanel.activatePanel(DOCK_NODE_CONTEXT_HELP, true);
        });

        dockPanel.setBackgroundComponent(centerPanel);
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
        Path path = JIPipeDesktop.openPath(this, getDesktopProjectWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Add new project-wide path/directory", HTMLText.EMPTY);
        if (path != null) {
            JIPipeProjectDirectories.DirectoryEntry entry = new JIPipeProjectDirectories.DirectoryEntry();
            entry.setPath(path);
            entry.setName(path.getFileName().toString());
            while (true) {
                if (JIPipeDesktopParameterFormPanel.showDialog(getDesktopProjectWorkbench(), entry, new MarkdownText("# Add new project-wide path/directory\n\n" +
                        "Please provide at least a unique key that identifies the path and allows to recall it from within the workflow. " +
                        "You can also request that the path must exist."), "Add new project-wide path/directory", JIPipeDesktopParameterFormPanel.DEFAULT_DIALOG_FLAGS)) {
                    if (StringUtils.isNullOrEmpty(entry.getKey())) {
                        JOptionPane.showMessageDialog(this, "No key was provided", "Add new project-wide path/directory", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                    if (getProject().getMetadata().getDirectories().getDirectoriesAsInstance().stream().anyMatch(e -> Objects.equals(e.getKey(), entry.getKey()))) {
                        JOptionPane.showMessageDialog(this, "The key already exists", "Add new project-wide path/directory", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                    getProject().getMetadata().getDirectories().getDirectories().addFromTemplate(entry);
                    refreshParameters();
                    break;
                }
                else {
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

    private static class ArtifactUpgrade {
        private final JIPipeArtifactEnvironment environment;
        private final JIPipeArtifact current;
        private final List<JIPipeArtifact> revisionUpgrades;
        private final List<JIPipeArtifact> accelerationUpgrades;

        public ArtifactUpgrade(JIPipeArtifactEnvironment environment, JIPipeArtifact current, List<JIPipeArtifact> revisionUpgrades, List<JIPipeArtifact> accelerationUpgrades) {
            this.environment = environment;
            this.current = current;
            this.revisionUpgrades = revisionUpgrades;
            this.accelerationUpgrades = accelerationUpgrades;
        }
    }


}
