package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.MutableACAQRunConfiguration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.*;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.running.ACAQRunExecuterUI;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueue;
import org.hkijena.acaq5.ui.testbench.ACAQTestBenchUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

public class ACAQTestBenchSetupUI extends ACAQUIPanel {

    private ACAQAlgorithm algorithm;
    private JPanel setupPanel;
    private JPanel validationReportPanel;
    private ACAQValidityReportUI validationReportUI;
    private ACAQRun currentRun;

    public ACAQTestBenchSetupUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithm algorithm) {
        super(workbenchUI);
        this.algorithm = algorithm;

        setLayout(new BorderLayout());
        this.validationReportUI = new ACAQValidityReportUI();

        initializeValidationReportUI();
        initializeSetupPanel();

        tryShowSetupPanel();

        ACAQRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initializeValidationReportUI() {
        validationReportPanel = new JPanel();
        validationReportPanel.setLayout(new BorderLayout());
        validationReportUI = new ACAQValidityReportUI();
        DocumentedComponent pane = new DocumentedComponent(true, "documentation/testbench.md", validationReportUI);
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

        FormPanel formPanel = new FormPanel("documentation/testbench.md", true);
        setupPanel.add(formPanel, BorderLayout.CENTER);

        // Allow the user to select one sample
        JComboBox<String> sampleSelection = new JComboBox<String>(getProject().getSamples().keySet().toArray(new String[0]));
        formPanel.addToForm(sampleSelection, new JLabel("Sample"), null);

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
        generateButton.addActionListener(e -> generateTestBench(sampleSelection.getSelectedItem().toString(),
                outputFolderSelection.getPath()));
        toolBar.add(generateButton);

        setupPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void tryShowSetupPanel() {
        ACAQValidityReport report = new ACAQValidityReport();
        getProject().reportValidity(report);
        removeAll();
        if(report.isValid()) {
            add(setupPanel, BorderLayout.CENTER);
        }
        else {
            add(validationReportPanel, BorderLayout.CENTER);
            validationReportUI.setReport(report);
        }
        revalidate();
        repaint();
    }

    private void openError(Exception exception) {
        removeAll();

        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        JTextArea errorArea = new JTextArea(writer.toString());
        errorArea.setEditable(false);

        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.add(new JScrollPane(errorArea), BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar(); toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Retry", UIUtils.getIconFromResources("refresh.png"));
        refreshButton.addActionListener(e -> tryShowSetupPanel());
        toolBar.add(refreshButton);
        errorPanel.add(toolBar, BorderLayout.NORTH);

        add(errorPanel, BorderLayout.CENTER);

        revalidate();
    }

    private void generateTestBench(String sample, Path outputPath) {
        MutableACAQRunConfiguration configuration = new MutableACAQRunConfiguration();
        configuration.setFlushingEnabled(true);
        configuration.setFlushingKeepsDataEnabled(true);
        configuration.setOutputPath(outputPath.resolve("initial"));
        configuration.setEndAlgorithm(algorithm);
        configuration.setSampleRestrictions(new HashSet<>(Arrays.asList(sample)));

        currentRun = new ACAQRun(getProject(), configuration);

        removeAll();
        ACAQRunExecuterUI executerUI = new ACAQRunExecuterUI(currentRun);
        add(executerUI, BorderLayout.SOUTH);
        revalidate();
        repaint();
        executerUI.startRun();
    }

    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if(event.getRun() == currentRun) {
            tryShowSetupPanel();

            try {
                ACAQTestBenchUI testBenchUI = new ACAQTestBenchUI(getWorkbenchUI(), algorithm, currentRun);
                String name = "Testbench: " + algorithm.getName();
                getWorkbenchUI().getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("testbench.png"),
                        testBenchUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
                getWorkbenchUI().getDocumentTabPane().switchToLastTab();
                currentRun = null;
            }
            catch (Exception e) {
                openError(e);
            }
        }
    }

    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if(event.getRun() == currentRun) {
            openError(event.getException());
        }
    }
}
