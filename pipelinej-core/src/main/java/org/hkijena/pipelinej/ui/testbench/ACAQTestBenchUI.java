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

package org.hkijena.pipelinej.ui.testbench;

import com.google.common.eventbus.Subscribe;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.testbench.ACAQTestBench;
import org.hkijena.pipelinej.api.testbench.ACAQTestbenchSnapshot;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.pipelinej.ui.components.ColorIcon;
import org.hkijena.pipelinej.ui.components.ConfirmingButton;
import org.hkijena.pipelinej.ui.components.MarkdownDocument;
import org.hkijena.pipelinej.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.pipelinej.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.pipelinej.ui.parameters.ParameterPanel;
import org.hkijena.pipelinej.ui.resultanalysis.ACAQResultUI;
import org.hkijena.pipelinej.ui.running.ACAQRunnerQueue;
import org.hkijena.pipelinej.utils.TooltipUtils;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * UI for {@link ACAQTestBench}
 */
public class ACAQTestBenchUI extends ACAQProjectWorkbenchPanel {

    private final ACAQTestBench testBench;
    private JComboBox<ACAQTestbenchSnapshot> backupSelection;
    private JButton newTestButton;
    private JSplitPane splitPane;

    /**
     * @param workbenchUI the workbench
     * @param testBench   the testbench
     */
    public ACAQTestBenchUI(ACAQProjectWorkbench workbenchUI, ACAQTestBench testBench) {
        super(workbenchUI);
        this.testBench = testBench;

        initialize();
        updateBackupSelection();
        loadBackup(testBench.getLatestBackup());

        ACAQRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolbar();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        String compartmentName = getProject().getCompartments().get(testBench.getProjectAlgorithm().getCompartment()).getName();
        JLabel compartmentInfo = new JLabel(compartmentName, UIUtils.getIconFromResources("graph-compartment.png"), JLabel.LEFT);
        toolBar.add(compartmentInfo);

        toolBar.add(Box.createHorizontalStrut(8));

        JLabel algorithmInfo = new JLabel(testBench.getProjectAlgorithm().getName(), new ColorIcon(16, 16,
                UIUtils.getFillColorFor(testBench.getProjectAlgorithm().getDeclaration())), JLabel.LEFT);
        algorithmInfo.setToolTipText(TooltipUtils.getAlgorithmTooltip(testBench.getProjectAlgorithm().getDeclaration()));
        toolBar.add(algorithmInfo);

        toolBar.add(Box.createHorizontalGlue());

        backupSelection = new JComboBox<>();
        backupSelection.setRenderer(new ACAQDataSlotBackupListCellRenderer());
        backupSelection.addActionListener(e -> {
            if (backupSelection.getSelectedItem() != null)
                loadBackup((ACAQTestbenchSnapshot) backupSelection.getSelectedItem());
        });
        toolBar.add(backupSelection);

        JButton clearBackupsButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        clearBackupsButton.setToolTipText("Remove all other time points");
        clearBackupsButton.addActionListener(e -> clearBackups());
        toolBar.add(clearBackupsButton);

        JButton renameButton = new JButton(UIUtils.getIconFromResources("label.png"));
        renameButton.setToolTipText("Label test");
        renameButton.addActionListener(e -> renameCurrentBackup());
        toolBar.add(renameButton);

        JButton copyParametersButton = new ConfirmingButton("Apply parameters", UIUtils.getIconFromResources("upload.png"));
        copyParametersButton.setToolTipText("Copies the current parameters to the algorithm that was used to create this testbench.");
        copyParametersButton.addActionListener(e -> copyParameters());
        toolBar.add(copyParametersButton);

        toolBar.add(Box.createHorizontalStrut(8));

        newTestButton = new JButton("New test", UIUtils.getIconFromResources("run.png"));
        newTestButton.addActionListener(e -> newTest());
        toolBar.add(newTestButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void copyParameters() {
        if (backupSelection.getSelectedItem() instanceof ACAQTestbenchSnapshot) {
            ACAQTestbenchSnapshot backup = (ACAQTestbenchSnapshot) backupSelection.getSelectedItem();
            backup.getAlgorithmBackup(testBench.getBenchedAlgorithm()).restoreParameters(testBench.getProjectAlgorithm());
            getProjectWorkbench().sendStatusBarText("Copied parameters from quick run to " + testBench.getProjectAlgorithm().getName());
        }
    }

    private void renameCurrentBackup() {
        if (backupSelection.getSelectedItem() instanceof ACAQTestbenchSnapshot) {
            ACAQTestbenchSnapshot backup = (ACAQTestbenchSnapshot) backupSelection.getSelectedItem();
            String newName = JOptionPane.showInputDialog(this, "Please enter a label", backup.getLabel());
            backup.setLabel(newName);
            updateBackupSelection();
        }
    }

    private void clearBackups() {
        Object current = backupSelection.getSelectedItem();
        ACAQTestbenchSnapshot initial = testBench.getInitialBackup();
        testBench.getBackupList().removeIf(b -> b != current);
        updateBackupSelection();
    }

    private void updateBackupSelection() {
        Object currentSelection = backupSelection.getSelectedItem();
        DefaultComboBoxModel<ACAQTestbenchSnapshot> model = new DefaultComboBoxModel<>(testBench.getBackupList().toArray(new ACAQTestbenchSnapshot[0]));
        if (currentSelection != null && testBench.getBackupList().contains(currentSelection))
            model.setSelectedItem(currentSelection);
        backupSelection.setModel(model);
    }

    private void newTest() {

        // Check if we are still valid
        ACAQValidityReport report = new ACAQValidityReport();
        testBench.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }

        testBench.newTest();

        newTestButton.setEnabled(false);
        ACAQRunnerQueue.getInstance().enqueue(testBench);
    }

    /**
     * Triggered when the quick run finished calculating
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == testBench) {
            newTestButton.setEnabled(true);
            testBench.createBackup();
            updateBackupSelection();
            backupSelection.setSelectedItem(testBench.getLatestBackup());
        }
    }

    /**
     * Triggered when the quick run finished calculating
     *
     * @param event Generated event
     */
    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getRun() == testBench) {
            newTestButton.setEnabled(true);
            UIUtils.openErrorDialog(this, event.getException());
        }
    }

    private void loadBackup(ACAQTestbenchSnapshot backup) {

        int dividerLocation = splitPane.getDividerLocation();

        backup.restore();

        ParameterPanel parameters = new ParameterPanel(getProjectWorkbench(), testBench.getBenchedAlgorithm(),
                MarkdownDocument.fromPluginResource("documentation/testbench.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SCROLLING);
        splitPane.setLeftComponent(parameters);

        ACAQResultUI resultUI = new ACAQResultUI(getProjectWorkbench(), testBench.getTestBenchRun());
        splitPane.setRightComponent(resultUI);

        revalidate();
        repaint();

        splitPane.setDividerLocation(dividerLocation);
    }
}
