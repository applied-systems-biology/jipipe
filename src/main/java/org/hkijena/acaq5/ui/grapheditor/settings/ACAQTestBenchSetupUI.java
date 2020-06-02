package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.testbench.ACAQTestbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ACAQValidityReportUI;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.DocumentedComponent;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.UserFriendlyErrorUI;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.running.ACAQRunExecuterUI;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueue;
import org.hkijena.acaq5.ui.testbench.ACAQTestBenchUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * UI for generating {@link ACAQTestbench}
 */
public class ACAQTestBenchSetupUI extends ACAQProjectWorkbenchPanel {

    private ACAQAlgorithm algorithm;
    private JPanel setupPanel;
    private JPanel validationReportPanel;
    private ACAQValidityReportUI validationReportUI;
    private ACAQTestbench currentTestBench;

    /**
     * @param workbenchUI the workbench
     * @param algorithm   the target algorithm
     */
    public ACAQTestBenchSetupUI(ACAQProjectWorkbench workbenchUI, ACAQAlgorithm algorithm) {
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

        FormPanel formPanel = new FormPanel(MarkdownDocument.fromPluginResource("documentation/testbench.md"),
                FormPanel.WITH_SCROLLING);
        setupPanel.add(formPanel, BorderLayout.CENTER);

        // Let the user choose where temporary data is saved
        FileSelection outputFolderSelection = new FileSelection();
        outputFolderSelection.setIoMode(FileSelection.IOMode.Open);
        outputFolderSelection.setPathMode(FileSelection.PathMode.DirectoriesOnly);
        try {
            outputFolderSelection.setPath(Files.createTempDirectory("ACAQ5"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        formPanel.addToForm(outputFolderSelection, new JLabel("Temp. output folder"), null);

        formPanel.addVerticalGlue();

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton generateButton = new JButton("Create", UIUtils.getIconFromResources("run.png"));
        generateButton.addActionListener(e -> generateTestBench(outputFolderSelection.getPath()));
        toolBar.add(generateButton);

        setupPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void tryShowSetupPanel() {
        ACAQValidityReport report = new ACAQValidityReport();
        getProject().reportValidity(report);

        Set<ACAQAlgorithm> algorithmsWithMissingInput = getProject().getGraph().getAlgorithmsWithMissingInput();
        if (algorithmsWithMissingInput.contains(algorithm)) {
            report.forCategory("Testbench").reportIsInvalid(
                    "Selected algorithm is missing inputs!",
                    "The selected algorithm would not be executed, as it is missing input data. " +
                            "You have to ensure that all input slots are assigned for the selected algorithm and its dependencies.",
                    "Please check if all input slots are assigned. Also check all dependency algorithms.",
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

    private void generateTestBench(Path outputPath) {

        ACAQValidityReport report = new ACAQValidityReport();
        getProject().reportValidity(report);
        if (!report.isValid()) {
            tryShowSetupPanel();
            return;
        }

        currentTestBench = new ACAQTestbench(getProject(), algorithm, outputPath);

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
