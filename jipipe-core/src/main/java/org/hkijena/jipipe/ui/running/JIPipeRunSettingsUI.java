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

package org.hkijena.jipipe.ui.running;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.JIPipeValidityReportUI;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.components.UserFriendlyErrorUI;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Settings UI for {@link org.hkijena.jipipe.api.JIPipeRunSettings}
 */
public class JIPipeRunSettingsUI extends JIPipeProjectWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private JIPipeProjectRun run;

    /**
     * @param workbenchUI workbench UI
     */
    public JIPipeRunSettingsUI(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
        JIPipeRunnerQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JIPipeValidationReport report = new JIPipeValidationReport();
        getProjectWorkbench().getProject().reportValidity(new UnspecifiedValidationReportContext(), report);
        if (report.isValid()) {
            initializeSetupGUI();
        } else {
            initializeValidityCheckUI(report);
        }
    }

    private void initializeValidityCheckUI(JIPipeValidationReport report) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(8, 8));
        JIPipeValidityReportUI reportUI = new JIPipeValidityReportUI(getWorkbench(), false);
        reportUI.setReport(report);

        MarkdownReader help = new MarkdownReader(false);
        help.setDocument(MarkdownDocument.fromPluginResource("documentation/validation.md", new HashMap<>()));

        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.add(reportUI, BorderLayout.CENTER);

        // Create button panel
        MessagePanel messagePanel = new MessagePanel();

        JButton runButton = new JButton("Retry", UIUtils.getIconFromResources("actions/view-refresh.png"));
        runButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        runButton.addActionListener(e -> {
            report.clear();
            getProjectWorkbench().getProject().reportValidity(new UnspecifiedValidationReportContext(), report);
            getProjectWorkbench().sendStatusBarText("Re-validated JIPipe project");
            if (report.isValid())
                initializeSetupGUI();
            else
                reportUI.setReport(report);
        });
        messagePanel.addMessage(MessagePanel.MessageType.Error, "There are errors in your project that prevent a run", false, false, runButton);
        reportPanel.add(messagePanel, BorderLayout.NORTH);

        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, reportPanel, help, AutoResizeSplitPane.RATIO_3_TO_1);
        panel.add(splitPane, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);
    }

    private void initializeSetupGUI() {

        try {
            JIPipeRunSettings settings = new JIPipeRunSettings();
            settings.setOutputPath(RuntimeSettings.generateTempDirectory(""));
            run = new JIPipeProjectRun(getProjectWorkbench().getProject(), settings);
        } catch (Exception e) {
            openError(e);
            return;
        }

        removeAll();

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, new AutoResizeSplitPane.DynamicSidebarRatio(600, false));
        AutoResizeSplitPane splitPane1 = new AutoResizeSplitPane(AutoResizeSplitPane.TOP_BOTTOM, AutoResizeSplitPane.RATIO_1_TO_1);
        DocumentTabPane tabPane = new DocumentTabPane();
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tabPane, BorderLayout.CENTER);

        MessagePanel messagePanel = new MessagePanel();

        splitPane.setLeftComponent(centerPanel);
        splitPane.setRightComponent(splitPane1);
        splitPane.applyRatio();

        splitPane1.setTopComponent(messagePanel);
        splitPane1.setBottomComponent(new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/run.md", new HashMap<>())));
        splitPane1.applyRatio();

        JButton runButton = new JButton("Run now", UIUtils.getIconFromResources("actions/run-build.png"));
        runButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        runButton.addActionListener(e -> runNow());
        messagePanel.addMessage(MessagePanel.MessageType.Success, "Please review the settings on the left-hand side. Then proceed to click the following button.", false, false, runButton);
        messagePanel.addMessage(MessagePanel.MessageType.Gray, "Please note that a copy of the project was created. Feel free to schedule multiple runs with different pipeline configurations.", true, false);


        // General settings tab
        tabPane.addTab("General", UIUtils.getIconFromResources("actions/configure.png"), new ParameterPanel(getProjectWorkbench(),
                run.getConfiguration(),
               new MarkdownDocument(),
                ParameterPanel.WITH_SCROLLING), DocumentTabPane.CloseMode.withoutCloseButton);

        Set<JIPipeGraphNode> algorithmsWithMissingInput = getProjectWorkbench().getProject().getGraph().getDeactivatedNodes(true);
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

    private void createWrittenResultsPanel(DocumentTabPane tabPane, Set<JIPipeDataSlot> remainingOutputs) {
        FormPanel infoPanel = new FormPanel(FormPanel.WITH_SCROLLING);
        tabPane.addTab("Written results", UIUtils.getIconFromResources("actions/save.png"), infoPanel, DocumentTabPane.CloseMode.withoutCloseButton);
        createOutputsManagerPanel(infoPanel, remainingOutputs);
        infoPanel.addVerticalGlue();
    }

    private void createLargeIntermediateResultsPanel(MessagePanel messagePanel, DocumentTabPane tabPane, Set<JIPipeDataSlot> heavyIntermediateOutputs) {
        FormPanel infoPanel = new FormPanel(FormPanel.WITH_SCROLLING);
        messagePanel.addMessage(MessagePanel.MessageType.Gray,
                "There are nodes that generate large intermediate results. Please go to the 'Performance optimization' tab if you want to disable writing the outputs to the cache/hard drive.",
                true,
                false);
        tabPane.addTab("Performance optimization", UIUtils.getIconFromResources("actions/speedometer.png"), infoPanel, DocumentTabPane.CloseMode.withoutCloseButton);
        createOutputsManagerPanel(infoPanel, heavyIntermediateOutputs);
        infoPanel.addVerticalGlue();
    }

    private void createSkippedNodesPanel(MessagePanel messagePanel, DocumentTabPane tabPane, Set<JIPipeGraphNode> algorithmsWithMissingInput) {
        JPanel infoPanel = new JPanel(new BorderLayout());
        messagePanel.addMessage(MessagePanel.MessageType.Warning,
                "Not all nodes can be executed, which may or may not be intended. Please review the 'Skipped nodes' tab.",
                true,
                false);
        tabPane.addTab("Skipped nodes", UIUtils.getIconFromResources("emblems/warning.png"), infoPanel, DocumentTabPane.CloseMode.withoutCloseButton);

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

    private void createOutputsManagerPanel(FormPanel formPanel, Set<JIPipeDataSlot> outputSlots) {
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
                        JCheckBox checkBox = new JCheckBox(outputSlot.getDisplayName(), true);
                        checkBox.addActionListener(e -> {
                            JIPipeGraphNode runAlgorithm = run.getGraph().getEquivalentAlgorithm(node);
                            runAlgorithm.getOutputSlot(outputSlot.getName()).getInfo().setSaveOutputs(checkBox.isSelected());
                        });
                        JLabel compartmentLabel = new JLabel(node.getCompartmentDisplayName(),
                                UIUtils.getIconFromResources("data-types/graph-compartment.png"), JLabel.LEFT);
                        contentPanel.add(new JLabel(JIPipe.getNodes().getIconFor(node.getInfo())), new GridBagConstraints() {
                            {
                                gridx = 0;
                                gridy = row;
                                insets = UIUtils.UI_PADDING;
                            }
                        });
                        contentPanel.add(checkBox, new GridBagConstraints() {
                            {
                                gridx = 1;
                                gridy = row;
                                weightx = 1;
                                fill = GridBagConstraints.HORIZONTAL;
                                insets = UIUtils.UI_PADDING;
                            }
                        });
                        contentPanel.add(compartmentLabel, new GridBagConstraints() {
                            {
                                gridx = 2;
                                gridy = row;
                                insets = UIUtils.UI_PADDING;
                                anchor = GridBagConstraints.WEST;
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
                    JIPipeGraphNode runAlgorithm = run.getGraph().getEquivalentAlgorithm(node);
                    runAlgorithm.getOutputSlot(slot.getName()).getInfo().setSaveOutputs(true);
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
                    JIPipeGraphNode runAlgorithm = run.getGraph().getEquivalentAlgorithm(node);
                    runAlgorithm.getOutputSlot(slot.getName()).getInfo().setSaveOutputs(false);
                }
            });
            toolBar.add(selectNoneButton);

            panel.add(toolBar, BorderLayout.NORTH);
            panel.add(contentPanel, BorderLayout.CENTER);
            panel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Button.borderColor")));

            formPanel.addWideToForm(panel, null);
        }
    }

    private void runNow() {
        removeAll();
        RuntimeSettings.getInstance().setDefaultRunThreads(run.getConfiguration().getNumThreads());
        JIPipeRunExecuterUI executerUI = new JIPipeRunExecuterUI(run);
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
        if (event.getRun() == run)
            openResults();
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
        UserFriendlyErrorUI errorUI = new UserFriendlyErrorUI(getWorkbench(), MarkdownDocument.fromPluginResource("documentation/run-error.md", new HashMap<>()),
                UserFriendlyErrorUI.WITH_SCROLLING);
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
        JIPipeResultUI resultUI = new JIPipeResultUI(getProjectWorkbench(), run);
        removeAll();
        add(resultUI, BorderLayout.CENTER);
        revalidate();
    }
}
