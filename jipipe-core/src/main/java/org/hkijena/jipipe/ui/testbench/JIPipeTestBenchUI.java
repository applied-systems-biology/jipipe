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

package org.hkijena.jipipe.ui.testbench;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.testbench.JIPipeTestBench;
import org.hkijena.jipipe.api.testbench.JIPipeTestbenchSnapshot;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.ConfirmingButton;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * UI for {@link JIPipeTestBench}
 */
public class JIPipeTestBenchUI extends JIPipeProjectWorkbenchPanel {

    private final JIPipeTestBench testBench;
    private JComboBox<JIPipeTestbenchSnapshot> backupSelection;
    private JButton newTestButton;
    private JSplitPane splitPane;

    /**
     * @param workbenchUI the workbench
     * @param testBench   the testbench
     */
    public JIPipeTestBenchUI(JIPipeProjectWorkbench workbenchUI, JIPipeTestBench testBench) {
        super(workbenchUI);
        this.testBench = testBench;

        initialize();
        updateBackupSelection();
        loadBackup(testBench.getLatestBackup());

        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
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

        String compartmentName = testBench.getProjectAlgorithm().getCompartmentDisplayName();
        JLabel compartmentInfo = new JLabel(compartmentName, UIUtils.getIconFromResources("data-types/graph-compartment.png"), JLabel.LEFT);
        toolBar.add(compartmentInfo);

        toolBar.add(Box.createHorizontalStrut(8));

        JLabel algorithmInfo = new JLabel(testBench.getProjectAlgorithm().getName(), new ColorIcon(16, 16,
                UIUtils.getFillColorFor(testBench.getProjectAlgorithm().getInfo())), JLabel.LEFT);
        algorithmInfo.setToolTipText(TooltipUtils.getAlgorithmTooltip(testBench.getProjectAlgorithm().getInfo()));
        toolBar.add(algorithmInfo);

        toolBar.add(Box.createHorizontalGlue());

        backupSelection = new JComboBox<>();
        backupSelection.setRenderer(new JIPipeDataSlotBackupListCellRenderer());
        backupSelection.addActionListener(e -> {
            if (backupSelection.getSelectedItem() != null)
                loadBackup((JIPipeTestbenchSnapshot) backupSelection.getSelectedItem());
        });
        toolBar.add(backupSelection);

        JButton clearBackupsButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        clearBackupsButton.setToolTipText("Remove all other time points");
        clearBackupsButton.addActionListener(e -> clearBackups());
        toolBar.add(clearBackupsButton);

        JButton renameButton = new JButton(UIUtils.getIconFromResources("actions/tag.png"));
        renameButton.setToolTipText("Label test");
        renameButton.addActionListener(e -> renameCurrentBackup());
        toolBar.add(renameButton);

        JButton copyParametersButton = new ConfirmingButton("Apply parameters", UIUtils.getIconFromResources("actions/dialog-apply.png"));
        copyParametersButton.setToolTipText("Copies the current parameters to the algorithm that was used to create this testbench.");
        copyParametersButton.addActionListener(e -> copyParameters());
        toolBar.add(copyParametersButton);

        toolBar.add(Box.createHorizontalStrut(8));

        newTestButton = new JButton("New test", UIUtils.getIconFromResources("actions/run-build.png"));
        newTestButton.addActionListener(e -> newTest());
        toolBar.add(newTestButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void copyParameters() {
        if (backupSelection.getSelectedItem() instanceof JIPipeTestbenchSnapshot) {
            JIPipeTestbenchSnapshot backup = (JIPipeTestbenchSnapshot) backupSelection.getSelectedItem();
            backup.getAlgorithmBackup(testBench.getBenchedAlgorithm()).restoreParameters(testBench.getProjectAlgorithm());
            getProjectWorkbench().sendStatusBarText("Copied parameters from quick run to " + testBench.getProjectAlgorithm().getName());
        }
    }

    private void renameCurrentBackup() {
        if (backupSelection.getSelectedItem() instanceof JIPipeTestbenchSnapshot) {
            JIPipeTestbenchSnapshot backup = (JIPipeTestbenchSnapshot) backupSelection.getSelectedItem();
            String newName = JOptionPane.showInputDialog(this, "Please enter a label", backup.getLabel());
            backup.setLabel(newName);
            updateBackupSelection();
        }
    }

    private void clearBackups() {
        Object current = backupSelection.getSelectedItem();
        JIPipeTestbenchSnapshot initial = testBench.getInitialBackup();
        testBench.getBackupList().removeIf(b -> b != current);
        updateBackupSelection();
    }

    private void updateBackupSelection() {
        Object currentSelection = backupSelection.getSelectedItem();
        DefaultComboBoxModel<JIPipeTestbenchSnapshot> model = new DefaultComboBoxModel<>(testBench.getBackupList().toArray(new JIPipeTestbenchSnapshot[0]));
        if (currentSelection != null && testBench.getBackupList().contains(currentSelection))
            model.setSelectedItem(currentSelection);
        backupSelection.setModel(model);
    }

    private void newTest() {

        // Check if we are still valid
        JIPipeValidityReport report = new JIPipeValidityReport();
        testBench.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }

        testBench.newTest();

        newTestButton.setEnabled(false);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
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

    private void loadBackup(JIPipeTestbenchSnapshot backup) {

        int dividerLocation = splitPane.getDividerLocation();

        backup.restore();

        ParameterPanel parameters = new ParameterPanel(getProjectWorkbench(), testBench.getBenchedAlgorithm(),
                MarkdownDocument.fromPluginResource("documentation/testbench.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW | ParameterPanel.WITH_SCROLLING);
        splitPane.setLeftComponent(parameters);

        JIPipeResultUI resultUI = new JIPipeResultUI(getProjectWorkbench(), testBench.getTestBenchRun());
        splitPane.setRightComponent(resultUI);

        revalidate();
        repaint();

        splitPane.setDividerLocation(dividerLocation);
    }
}
