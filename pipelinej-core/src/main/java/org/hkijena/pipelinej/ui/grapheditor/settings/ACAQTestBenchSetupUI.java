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

package org.hkijena.pipelinej.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.testbench.ACAQTestBench;
import org.hkijena.pipelinej.api.testbench.ACAQTestBenchSettings;
import org.hkijena.pipelinej.extensions.settings.RuntimeSettings;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.pipelinej.ui.components.*;
import org.hkijena.pipelinej.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.pipelinej.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.pipelinej.ui.parameters.ParameterPanel;
import org.hkijena.pipelinej.ui.running.ACAQRunExecuterUI;
import org.hkijena.pipelinej.ui.running.ACAQRunnerQueue;
import org.hkijena.pipelinej.ui.testbench.ACAQTestBenchUI;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.function.Consumer;

/**
 * UI for generating {@link ACAQTestBench}
 */
public class ACAQTestBenchSetupUI extends ACAQProjectWorkbenchPanel {

    boolean showNextResults;
    private ACAQGraphNode algorithm;
    private JPanel setupPanel;
    private JPanel validationReportPanel;
    private ACAQValidityReportUI validationReportUI;
    private ACAQTestBenchSettings currentSettings;
    private ACAQTestBench currentTestBench;
    private Consumer<ACAQTestBench> nextRunOnSuccess;

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

    /**
     * Attempts to setup and run the quick run automatically and run a function when finished
     *
     * @param showResults show results after successful run
     * @param settings    settings
     * @param onSuccess   called if successful
     * @return if the initial validation failed
     */
    public boolean tryAutoRun(boolean showResults, ACAQTestBenchSettings settings, Consumer<ACAQTestBench> onSuccess) {
        if (!validateOrShowError())
            return false;
        currentSettings = settings;
        nextRunOnSuccess = onSuccess;
        generateTestBench(showResults);
        return true;
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

        JButton runOnly = new JButton("Run", UIUtils.getIconFromResources("run.png"));
        runOnly.addActionListener(e -> generateTestBench(false));
        toolBar.add(runOnly);

        JButton runAndOpen = new JButton("Run & open results", UIUtils.getIconFromResources("run.png"));
        runAndOpen.addActionListener(e -> generateTestBench(true));
        toolBar.add(runAndOpen);

        setupPanel.add(toolBar, BorderLayout.NORTH);
    }

    private boolean validateOrShowError() {
        ACAQValidityReport report = new ACAQValidityReport();
        getProject().reportValidity(report, algorithm);

        Set<ACAQGraphNode> algorithmsWithMissingInput = getProject().getGraph().getDeactivatedAlgorithms();
        if (algorithmsWithMissingInput.contains(algorithm)) {
            report.forCategory("Test Bench").reportIsInvalid(
                    "Selected algorithm is deactivated or missing inputs!",
                    "The selected algorithm would not be executed, as it is deactivated or missing input data. " +
                            "You have to ensure that all input slots are assigned for the selected algorithm and its dependencies.",
                    "Please check if the parameter 'Enabled' is checked. Please check if all input slots are assigned. Also check all dependency algorithms.",
                    algorithm
            );
        }
        if (report.isValid())
            return true;

        // Replace by error UI
        removeAll();
        add(validationReportPanel, BorderLayout.CENTER);
        validationReportUI.setReport(report);
        revalidate();
        repaint();
        return false;
    }

    private void tryShowSetupPanel() {
        if (validateOrShowError()) {
            removeAll();
            add(setupPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
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

    private void generateTestBench(boolean showResults) {

        ACAQValidityReport report = new ACAQValidityReport();
        getProject().reportValidity(report, algorithm);
        if (!report.isValid()) {
            tryShowSetupPanel();
            return;
        }

        currentTestBench = new ACAQTestBench(getProject(), algorithm, currentSettings);
        RuntimeSettings.getInstance().setDefaultTestBenchThreads(currentSettings.getNumThreads());
        showNextResults = showResults;

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

            if (showNextResults) {
                try {
                    ACAQTestBenchUI testBenchUI = new ACAQTestBenchUI(getProjectWorkbench(), currentTestBench);
                    String name = "Quick run: " + algorithm.getName();
                    getProjectWorkbench().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("testbench.png"),
                            testBenchUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
                    getProjectWorkbench().getDocumentTabPane().switchToLastTab();
                    currentTestBench = null;
                } catch (Exception e) {
                    openError(e);
                }
            } else {
                if (nextRunOnSuccess != null) {
                    nextRunOnSuccess.accept(currentTestBench);
                    nextRunOnSuccess = null;
                }
                currentTestBench = null;
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
