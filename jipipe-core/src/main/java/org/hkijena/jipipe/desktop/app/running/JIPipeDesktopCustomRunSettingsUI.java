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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.categories.ExportNodeTypeCategory;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultUI;
import org.hkijena.jipipe.desktop.commons.components.*;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Settings UI for {@link JIPipeGraphRunConfiguration}
 */
public class JIPipeDesktopCustomRunSettingsUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private JIPipeGraphRun run;

    /**
     * @param workbenchUI workbench UI
     */
    public JIPipeDesktopCustomRunSettingsUI(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JIPipeValidationReport report = new JIPipeValidationReport();
        getDesktopProjectWorkbench().getProject().reportValidity(new UnspecifiedValidationReportContext(), report);
        if (report.isValid()) {
            initializeSetupGUI();
        } else {
            initializeValidityCheckUI(report);
        }
    }

    private void initializeValidityCheckUI(JIPipeValidationReport report) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(8, 8));
        JIPipeDesktopValidityReportUI reportUI = new JIPipeDesktopValidityReportUI(getDesktopWorkbench(), false);
        reportUI.setReport(report);

        JIPipeDesktopMarkdownReader help = new JIPipeDesktopMarkdownReader(false);
        help.setDocument(MarkdownText.fromPluginResource("documentation/validation.md", new HashMap<>()));

        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.add(reportUI, BorderLayout.CENTER);

        // Create button panel
        JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();

        JButton runButton = new JButton("Retry", UIUtils.getIconFromResources("actions/view-refresh.png"));
        runButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        runButton.addActionListener(e -> {
            report.clear();
            getDesktopProjectWorkbench().getProject().reportValidity(new UnspecifiedValidationReportContext(), report);
            getDesktopProjectWorkbench().sendStatusBarText("Re-validated JIPipe project");
            if (report.isValid())
                initializeSetupGUI();
            else
                reportUI.setReport(report);
        });
        messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Error, "There are errors in your project that prevent a run", false, false, runButton);
        reportPanel.add(messagePanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT, reportPanel, help, JIPipeDesktopSplitPane.RATIO_3_TO_1);
        panel.add(splitPane, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);
    }

    private void initializeSetupGUI() {

        try {
            JIPipeProject project = getDesktopProjectWorkbench().getProject();
            JIPipeGraphRunConfiguration settings = new JIPipeGraphRunConfiguration();
            settings.setOutputPath(project != null ? project.newTemporaryDirectory() : JIPipeRuntimeApplicationSettings.getTemporaryDirectory("run"));
            settings.setLoadFromCache(false);
            run = new JIPipeGraphRun(project, settings);
        } catch (Exception e) {
            openError(e);
            return;
        }

        removeAll();

        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT, new JIPipeDesktopSplitPane.DynamicSidebarRatio(600, false));
        JIPipeDesktopSplitPane splitPane1 = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.TOP_BOTTOM, JIPipeDesktopSplitPane.RATIO_1_TO_1);
        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Left);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tabPane, BorderLayout.CENTER);

        JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();

        splitPane.setLeftComponent(centerPanel);
        splitPane.setRightComponent(splitPane1);
        splitPane.applyRatio();

        splitPane1.setTopComponent(messagePanel);
        splitPane1.setBottomComponent(new JIPipeDesktopMarkdownReader(false, MarkdownText.fromPluginResource("documentation/run.md", new HashMap<>())));
        splitPane1.applyRatio();

        JButton runButton = new JButton("Run now", UIUtils.getIconFromResources("actions/run-build.png"));
        runButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        runButton.addActionListener(e -> runNow());

        messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Success, "Please review the settings on the left-hand side. Then proceed to click the following button.", false, false, runButton);

        boolean containsExporterNodes = getDesktopProjectWorkbench().getProject().getGraph().getGraphNodes().stream().anyMatch(node -> node.getInfo().getCategory() instanceof ExportNodeTypeCategory);
        if (containsExporterNodes) {
            JButton configureButton = new JButton("Only node-based export");
            configureButton.addActionListener(e -> {
                run.getConfiguration().setStoreToDisk(false);
                run.getConfiguration().setCleanupOutputsAfterFailure(true);
                run.getConfiguration().setCleanupOutputsAfterSuccess(true);
                run.getConfiguration().emitParameterChangedEvent("store-to-disk");
                run.getConfiguration().emitParameterChangedEvent("cleanup-outputs-after-success");
                run.getConfiguration().emitParameterChangedEvent("cleanup-outputs-after-failure");
                messagePanel.removeLastRow();
                messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Gray, "Node-based export was configured.", true, false);
                messagePanel.addVerticalGlue();
            });
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.InfoLight, "Your workflow contains exporter nodes. Click the following button to turn off JIPipe's automated result export.", true, true, configureButton);

        }

        messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Gray, "Please note that a copy of the project was created. Feel free to schedule multiple runs with different pipeline configurations.", true, false);


        // General settings tab
        tabPane.addTab("General", UIUtils.getIcon32FromResources("actions/configure.png"), new JIPipeDesktopParameterFormPanel(getDesktopProjectWorkbench(),
                run.getConfiguration(),
                new MarkdownText(),
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        Set<JIPipeGraphNode> algorithmsWithMissingInput = getDesktopProjectWorkbench().getProject().getGraph().getDeactivatedNodes(true);
        if (!algorithmsWithMissingInput.isEmpty()) {
            createSkippedNodesPanel(messagePanel, tabPane, algorithmsWithMissingInput);
        }

        Set<JIPipeDataSlot> heavyIntermediateOutputs = getProject().getHeavyIntermediateAlgorithmOutputSlots();
        for (JIPipeGraphNode node : algorithmsWithMissingInput) {
            node.getOutputSlots().forEach(heavyIntermediateOutputs::remove);
        }
        if (!heavyIntermediateOutputs.isEmpty()) {
            createLargeIntermediateResultsPanel(messagePanel, tabPane, heavyIntermediateOutputs);
        }
        Set<JIPipeDataSlot> remainingOutputs = getProject().getGraph().getSlotNodes().stream().filter(slot -> slot.getSlotType() == JIPipeSlotType.Output).collect(Collectors.toSet());
        if (!remainingOutputs.isEmpty()) {
            createWrittenResultsPanel(tabPane, remainingOutputs);
        }

        add(splitPane, BorderLayout.CENTER);

        messagePanel.addVerticalGlue();
        revalidate();
    }

    private void createWrittenResultsPanel(JIPipeDesktopTabPane tabPane, Set<JIPipeDataSlot> remainingOutputs) {
        JIPipeDesktopFormPanel infoPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        tabPane.addTab("Exported", UIUtils.getIcon32FromResources("actions/stock_save.png"), infoPanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        createOutputsManagerPanel(infoPanel, remainingOutputs);
        infoPanel.addVerticalGlue();
    }

    private void createLargeIntermediateResultsPanel(JIPipeDesktopMessagePanel messagePanel, JIPipeDesktopTabPane tabPane, Set<JIPipeDataSlot> heavyIntermediateOutputs) {
        JIPipeDesktopFormPanel infoPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Gray,
                "There are nodes that generate large intermediate results. Please go to the 'Performance optimization' tab if you want to disable writing the outputs to the cache/hard drive.",
                true,
                false);
        tabPane.addTab("Performance", UIUtils.getIcon32FromResources("actions/speedometer.png"), infoPanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        createOutputsManagerPanel(infoPanel, heavyIntermediateOutputs);
        infoPanel.addVerticalGlue();
    }

    private void createSkippedNodesPanel(JIPipeDesktopMessagePanel messagePanel, JIPipeDesktopTabPane tabPane, Set<JIPipeGraphNode> algorithmsWithMissingInput) {
        JPanel infoPanel = new JPanel(new BorderLayout());
        messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Warning,
                "Not all nodes can be executed, which may or may not be intended. Please review the 'Skipped nodes' tab.",
                true,
                false);
        tabPane.addTab("Skipped", UIUtils.getIcon32FromResources("emblems/vcs-conflicting.png"), infoPanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[]{"Compartment", "Node name"});
        for (JIPipeGraphNode algorithm : algorithmsWithMissingInput.stream().sorted(Comparator.comparing(JIPipeGraphNode::getCompartmentDisplayName))
                .collect(Collectors.toList())) {
            model.addRow(new Object[]{
                    StringUtils.createIconTextHTMLTable(algorithm.getCompartmentDisplayName(),
                            ResourceUtils.getPluginResource("icons/data-types/graph-compartment.png")),
                    algorithm.getName()
            });
        }
        JXTable table = new JXTable(model);
        table.packAll();
        table.setSortOrder(0, SortOrder.ASCENDING);
        UIUtils.fitRowHeights(table);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(table, BorderLayout.CENTER);
        panel.add(table.getTableHeader(), BorderLayout.NORTH);

        infoPanel.add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void createOutputsManagerPanel(JIPipeDesktopFormPanel formPanel, Set<JIPipeDataSlot> outputSlots) {
        if (!outputSlots.isEmpty()) {

            List<JCheckBox> checkBoxes = new ArrayList<>();
            JPanel contentPanel = new JPanel(new GridBagLayout());
            List<JIPipeGraphNode> traversed = getProject().getGraph().traverse();
            for (JIPipeGraphNode node : traversed) {
                if (!(node instanceof JIPipeAlgorithm))
                    continue;
                for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                    if (outputSlots.contains(outputSlot)) {
                        int row = checkBoxes.size();
                        JCheckBox checkBox = new JCheckBox(outputSlot.getName(), true);
                        checkBox.addActionListener(e -> {
                            JIPipeGraphNode runAlgorithm = run.getGraph().getEquivalentNode(node);
                            runAlgorithm.getOutputSlot(outputSlot.getName()).getInfo().setStoreToDisk(checkBox.isSelected());
                        });
                        JLabel compartmentLabel = new JLabel(node.getCompartmentDisplayName(),
                                UIUtils.getIconFromResources("data-types/graph-compartment.png"), JLabel.LEFT);
                        JLabel nodeLabel = new JLabel(node.getName(), JIPipe.getNodes().getIconFor(node.getInfo()), JLabel.LEFT);
                        contentPanel.add(nodeLabel, new GridBagConstraints() {
                            {
                                gridx = 1;
                                gridy = row;
                                fill = GridBagConstraints.HORIZONTAL;
                                insets = UIUtils.UI_PADDING;
                            }
                        });
                        contentPanel.add(checkBox, new GridBagConstraints() {
                            {
                                gridx = 2;
                                gridy = row;
                                weightx = 1;
                                fill = GridBagConstraints.HORIZONTAL;
                                insets = UIUtils.UI_PADDING;
                            }
                        });
                        contentPanel.add(compartmentLabel, new GridBagConstraints() {
                            {
                                gridx = 0;
                                gridy = row;
                                insets = UIUtils.UI_PADDING;
                                anchor = GridBagConstraints.WEST;
                                fill = GridBagConstraints.HORIZONTAL;
                            }
                        });
                        checkBoxes.add(checkBox);
                    }
                }
            }

            JPanel panel = new JPanel(new BorderLayout());

            JToolBar toolBar = new JToolBar();
            toolBar.add(Box.createHorizontalStrut(4));
            toolBar.add(new JLabel("Deselect items to disable saving to HDD"));
            toolBar.add(Box.createHorizontalGlue());

            JButton selectAllButton = new JButton("Select all", UIUtils.getIconFromResources("actions/stock_select-all.png"));
            selectAllButton.addActionListener(e -> {
                for (JCheckBox checkBox : checkBoxes) {
                    checkBox.setSelected(true);
                }
                for (JIPipeDataSlot slot : outputSlots) {
                    JIPipeGraphNode node = slot.getNode();
                    JIPipeGraphNode runAlgorithm = run.getGraph().getEquivalentNode(node);
                    runAlgorithm.getOutputSlot(slot.getName()).getInfo().setStoreToDisk(true);
                }
            });
            toolBar.add(selectAllButton);

            JButton selectNoneButton = new JButton("Select none", UIUtils.getIconFromResources("actions/cancel.png"));
            selectNoneButton.addActionListener(e -> {
                for (JCheckBox checkBox : checkBoxes) {
                    checkBox.setSelected(false);
                }
                for (JIPipeDataSlot slot : outputSlots) {
                    JIPipeGraphNode node = slot.getNode();
                    JIPipeGraphNode runAlgorithm = run.getGraph().getEquivalentNode(node);
                    runAlgorithm.getOutputSlot(slot.getName()).getInfo().setStoreToDisk(false);
                }
            });
            toolBar.add(selectNoneButton);

            panel.add(toolBar, BorderLayout.NORTH);
            panel.add(contentPanel, BorderLayout.CENTER);
            panel.setBorder(UIUtils.createControlBorder());

            formPanel.addWideToForm(panel, null);
        }
    }

    private void runNow() {
        removeAll();
        JIPipeRuntimeApplicationSettings.getInstance().setDefaultRunThreads(run.getConfiguration().getNumThreads());
        JIPipeDesktopRunExecuteUI executerUI = new JIPipeDesktopRunExecuteUI(getDesktopWorkbench(), run);
        add(executerUI, BorderLayout.CENTER);
        revalidate();
        repaint();
        executerUI.startRun();
    }

    /**
     * Triggered when the run is finished
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() == run) {
            openResults();
        }
    }

    /**
     * Triggered when the run is interrupted
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() == run)
            openError(event.getException());
    }

    private void openError(Throwable exception) {
        removeAll();
        JIPipeDesktopUserFriendlyErrorUI errorUI = new JIPipeDesktopUserFriendlyErrorUI(getDesktopWorkbench(), MarkdownText.fromPluginResource("documentation/run-error.md", new HashMap<>()),
                JIPipeDesktopUserFriendlyErrorUI.WITH_SCROLLING);
        errorUI.displayErrors(exception);
        errorUI.addVerticalGlue();
        add(errorUI, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton openFolderButton = new JButton("Open output folder", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openFolderButton.addActionListener(e -> openOutputFolder());
        toolBar.add(openFolderButton);
        if (Files.isRegularFile(run.getConfiguration().getOutputPath().resolve("log.txt"))) {
            JButton openLogButton = new JButton("Open log", UIUtils.getIconFromResources("actions/find.png"));
            openLogButton.addActionListener(e -> openLog());
            toolBar.add(openLogButton);
        }
        add(toolBar, BorderLayout.NORTH);

        revalidate();
        repaint();
    }

    private void openLog() {
        try {
            Desktop.getDesktop().open(run.getConfiguration().getOutputPath().resolve("log.txt").toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openOutputFolder() {
        try {
            Desktop.getDesktop().open(run.getConfiguration().getOutputPath().toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openResults() {
        JIPipeDesktopResultUI resultUI = new JIPipeDesktopResultUI(getDesktopProjectWorkbench(), run.getProject(), run.getConfiguration().getOutputPath());
        removeAll();
        add(resultUI, BorderLayout.CENTER);
        revalidate();
    }
}
