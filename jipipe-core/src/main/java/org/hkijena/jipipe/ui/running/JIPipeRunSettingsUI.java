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

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.JIPipeValidityReportUI;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.components.UserFriendlyErrorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultUI;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Settings UI for {@link org.hkijena.jipipe.api.JIPipeRunSettings}
 */
public class JIPipeRunSettingsUI extends JIPipeProjectWorkbenchPanel {

    private JIPipeRun run;

    /**
     * @param workbenchUI workbench UI
     */
    public JIPipeRunSettingsUI(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JIPipeValidityReport report = new JIPipeValidityReport();
        getProjectWorkbench().getProject().reportValidity(report);
        if (report.isValid()) {
            initializeSetupGUI();
        } else {
            initializeValidityCheckUI(report);
        }
    }

    private void initializeValidityCheckUI(JIPipeValidityReport report) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(8, 8));
        JIPipeValidityReportUI reportUI = new JIPipeValidityReportUI(false);
        reportUI.setReport(report);

        MarkdownReader help = new MarkdownReader(false);
        help.setDocument(MarkdownDocument.fromPluginResource("documentation/validation.md"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, reportUI, help);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        panel.add(splitPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton runButton = new JButton("Retry", UIUtils.getIconFromResources("actions/view-refresh.png"));
        runButton.addActionListener(e -> {
            report.clear();
            getProjectWorkbench().getProject().reportValidity(report);
            getProjectWorkbench().sendStatusBarText("Re-validated JIPipe project");
            if (report.isValid())
                initializeSetupGUI();
            else
                reportUI.setReport(report);
        });
        buttonPanel.add(runButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel, BorderLayout.CENTER);
    }

    private void initializeSetupGUI() {

        try {
            JIPipeRunSettings settings = new JIPipeRunSettings();
            settings.setOutputPath(RuntimeSettings.generateTempDirectory(""));
            run = new JIPipeRun(getProjectWorkbench().getProject(), settings);
        } catch (Exception e) {
            openError(e);
            return;
        }

        removeAll();
        JPanel setupPanel = new JPanel(new BorderLayout());
        ParameterPanel formPanel = new ParameterPanel(getProjectWorkbench(),
                run.getConfiguration(),
                MarkdownDocument.fromPluginResource("documentation/run.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);

        Set<JIPipeGraphNode> algorithmsWithMissingInput = getProjectWorkbench().getProject().getGraph().getDeactivatedAlgorithms();
        if (!algorithmsWithMissingInput.isEmpty()) {
            formPanel.removeLastRow();
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("Skipped algorithms", UIUtils.getIconFromResources("emblems/warning.png"));
            headerPanel.getDescriptionArea().setVisible(true);
            headerPanel.getDescriptionArea().setText("There are algorithms that will not be executed, as they are missing input data or are deactivated. " +
                    "If this is not intended, please check if the listed algorithms have all input slots connected and the affected algorithms are activated.");

            DefaultTableModel model = new DefaultTableModel();
            model.setColumnIdentifiers(new Object[]{"Compartment", "Algorithm name"});
            for (JIPipeGraphNode algorithm : algorithmsWithMissingInput.stream().sorted(Comparator.comparing(JIPipeGraphNode::getCompartment)).collect(Collectors.toList())) {
                model.addRow(new Object[]{
                        StringUtils.createIconTextHTMLTable(getProjectWorkbench().getProject().getCompartments().get(algorithm.getCompartment()).getName(),
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
            formPanel.addWideToForm(panel, null);

            formPanel.addVerticalGlue();
        }

        Set<JIPipeDataSlot> heavyIntermediateOutputs = getProject().getHeavyIntermediateAlgorithmOutputSlots();
        for (JIPipeGraphNode node : algorithmsWithMissingInput) {
            heavyIntermediateOutputs.removeAll(node.getOutputSlots());
        }
        if (!heavyIntermediateOutputs.isEmpty()) {
            formPanel.removeLastRow();
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("Large intermediate results", UIUtils.getIconFromResources("emblems/warning.png"));
            headerPanel.getDescriptionArea().setVisible(true);
            headerPanel.getDescriptionArea().setText("There are algorithms that look like that they only generate intermediate results, but generate potentially large amounts of data that would all be saved to the hard drive. " +
                    "You can deselect these outputs in the following list to disable saving outputs for them. They will still be executed, but their results will not be saved to the hard drive.");
            List<JCheckBox> checkBoxes = new ArrayList<>();
            JPanel contentPanel = new JPanel(new GridBagLayout());
            List<JIPipeGraphNode> traversed = getProject().getGraph().traverse();
            for (JIPipeGraphNode node : traversed) {
                if (!(node instanceof JIPipeAlgorithm))
                    continue;
                for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                    if (heavyIntermediateOutputs.contains(outputSlot)) {
                        int row = checkBoxes.size();
                        JCheckBox checkBox = new JCheckBox(outputSlot.getDisplayName(), true);
                        checkBox.addActionListener(e -> {
                            JIPipeGraphNode runAlgorithm = run.getGraph().getEquivalentAlgorithm(node);
                            runAlgorithm.getOutputSlot(outputSlot.getName()).getInfo().setSaveOutputs(checkBox.isSelected());
                        });
                        JLabel compartmentLabel = new JLabel(getProject().getCompartments().get(node.getCompartment()).getName(), UIUtils.getIconFromResources("data-types/graph-compartment.png"), JLabel.LEFT);
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
                for (JIPipeDataSlot slot : heavyIntermediateOutputs) {
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
                for (JIPipeDataSlot slot : heavyIntermediateOutputs) {
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
            formPanel.addVerticalGlue();
        }

        setupPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton runButton = new JButton("Run now", UIUtils.getIconFromResources("actions/run-build.png"));
        runButton.addActionListener(e -> runNow());
        buttonPanel.add(runButton);

        add(buttonPanel, BorderLayout.SOUTH);

        add(setupPanel, BorderLayout.CENTER);
        revalidate();
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
    @Subscribe
    public void onRunFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == run)
            openResults();
    }

    /**
     * Triggered when the run is interrupted
     *
     * @param event Generated event
     */
    @Subscribe
    public void onRunInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getRun() == run)
            openError(event.getException());
    }

    private void openError(Exception exception) {
        removeAll();
        UserFriendlyErrorUI errorUI = new UserFriendlyErrorUI(MarkdownDocument.fromPluginResource("documentation/run-error.md"),
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
