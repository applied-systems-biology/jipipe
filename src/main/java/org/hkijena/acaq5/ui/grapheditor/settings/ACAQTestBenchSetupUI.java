package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.testbench.ACAQTestBench;
import org.hkijena.acaq5.api.testbench.ACAQTestBenchSettings;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.*;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.running.ACAQRunExecuterUI;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueue;
import org.hkijena.acaq5.ui.testbench.ACAQTestBenchUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * UI for generating {@link ACAQTestBench}
 */
public class ACAQTestBenchSetupUI extends ACAQProjectWorkbenchPanel {

    private ACAQGraphNode algorithm;
    private JPanel setupPanel;
    private JPanel validationReportPanel;
    private ACAQValidityReportUI validationReportUI;
    private ACAQTestBenchSettings currentSettings;
    private ACAQTestBench currentTestBench;

    /**
     * @param workbenchUI the workbench
     * @param algorithm   the target algorithm
     */
    public ACAQTestBenchSetupUI(ACAQProjectWorkbench workbenchUI, ACAQGraphNode algorithm) {
        super(workbenchUI);
        this.algorithm = algorithm;

        setLayout(new BorderLayout());
        this.validationReportUI = new ACAQValidityReportUI(false);

        initializeValidationReportUI();
        initializeSetupPanel();

        tryShowSetupPanel();

        ACAQRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initializeValidationReportUI() {
        validationReportPanel = new JPanel();
        validationReportPanel.setLayout(new BorderLayout());
        validationReportUI = new ACAQValidityReportUI(false);
        DocumentedComponent pane = new DocumentedComponent(true,
                MarkdownDocument.fromPluginResource("documentation/testbench.md"),
                validationReportUI);
        validationReportPanel.add(pane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Retry", UIUtils.getIconFromResources("refresh.png"));
        refreshButton.addActionListener(e -> tryShowSetupPanel());
        toolBar.add(refreshButton);

        validationReportPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void initializeSetupPanel() {
        setupPanel = new JPanel();
        setupPanel.setLayout(new BorderLayout());

        currentSettings = new ACAQTestBenchSettings();
        ParameterPanel formPanel = new ParameterPanel(getWorkbench(), currentSettings,
                MarkdownDocument.fromPluginResource("documentation/testbench.md"), ParameterPanel.WITH_SCROLLING |
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW);
        setupPanel.add(formPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton generateButton = new JButton("Create", UIUtils.getIconFromResources("run.png"));
        generateButton.addActionListener(e -> generateTestBench());
        toolBar.add(generateButton);

        setupPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void tryShowSetupPanel() {
        ACAQValidityReport report = new ACAQValidityReport();
        getProject().reportValidity(report);

        Set<ACAQGraphNode> algorithmsWithMissingInput = getProject().getGraph().getDeactivatedAlgorithms();
        if (algorithmsWithMissingInput.contains(algorithm)) {
            report.forCategory("Testbench").reportIsInvalid(
                    "Selected algorithm is deactivated or missing inputs!",
                    "The selected algorithm would not be executed, as it is deactivated or missing input data. " +
                            "You have to ensure that all input slots are assigned for the selected algorithm and its dependencies.",
                    "Please check if the parameter 'Enabled' is checked. Please check if all input slots are assigned. Also check all dependency algorithms.",
                    algorithm
            );
        }

        removeAll();
        if (report.isValid()) {
            add(setupPanel, BorderLayout.CENTER);
        } else {
            add(validationReportPanel, BorderLayout.CENTER);
            validationReportUI.setReport(report);
        }
        revalidate();
        repaint();
    }

    private void openError(Exception exception) {
        removeAll();

        UserFriendlyErrorUI errorUI = new UserFriendlyErrorUI(null, UserFriendlyErrorUI.WITH_SCROLLING);
        errorUI.displayErrors(exception);
        errorUI.addVerticalGlue();

        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.add(errorUI, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Retry", UIUtils.getIconFromResources("refresh.png"));
        refreshButton.addActionListener(e -> tryShowSetupPanel());
        toolBar.add(refreshButton);
        errorPanel.add(toolBar, BorderLayout.NORTH);

        add(errorPanel, BorderLayout.CENTER);

        revalidate();
    }

    private void generateTestBench() {

        ACAQValidityReport report = new ACAQValidityReport();
        getProject().reportValidity(report);
        if (!report.isValid()) {
            tryShowSetupPanel();
            return;
        }

        currentTestBench = new ACAQTestBench(getProject(), algorithm, currentSettings);

        removeAll();
        ACAQRunExecuterUI executerUI = new ACAQRunExecuterUI(currentTestBench);
        add(executerUI, BorderLayout.CENTER);
        revalidate();
        repaint();
        executerUI.startRun();
    }

    /**
     * Triggered when a worker is finished
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == currentTestBench) {
            tryShowSetupPanel();

            try {
                ACAQTestBenchUI testBenchUI = new ACAQTestBenchUI(getProjectWorkbench(), currentTestBench);
                String name = "Testbench: " + algorithm.getName();
                getProjectWorkbench().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("testbench.png"),
                        testBenchUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
                getProjectWorkbench().getDocumentTabPane().switchToLastTab();
                currentTestBench = null;
            } catch (Exception e) {
                openError(e);
            }
        }
    }

    /**
     * Triggered when a worker is interrupted
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getRun() == currentTestBench) {
            openError(event.getException());
        }
    }
}
