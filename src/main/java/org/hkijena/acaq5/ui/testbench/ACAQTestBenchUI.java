package org.hkijena.acaq5.ui.testbench;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.testbench.ACAQAlgorithmBackup;
import org.hkijena.acaq5.api.testbench.ACAQTestbench;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.ConfirmingButton;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.events.ReloadSettingsRequestedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultUI;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueue;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class ACAQTestBenchUI extends ACAQUIPanel {

    private ACAQTestbench testbench;
    private JComboBox<ACAQAlgorithmBackup> backupSelection;
    private JButton newTestButton;
    private JSplitPane splitPane;

    public ACAQTestBenchUI(ACAQWorkbenchUI workbenchUI, ACAQTestbench testbench) {
        super(workbenchUI);
        this.testbench = testbench;

        // Do the initial backup
        testbench.createBackup();

        initialize();
        updateBackupSelection();
        loadBackup(testbench.getLatestBackup());

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

        String compartmentName = getProject().getCompartments().get(testbench.getTargetAlgorithm().getCompartment()).getName();
        JLabel compartmentInfo = new JLabel(compartmentName, UIUtils.getIconFromResources("graph-compartment.png"), JLabel.LEFT);
        toolBar.add(compartmentInfo);

        toolBar.add(Box.createHorizontalStrut(8));

        JLabel algorithmInfo = new JLabel(testbench.getTargetAlgorithm().getName(), new ColorIcon(16, 16,
                UIUtils.getFillColorFor(testbench.getTargetAlgorithm().getDeclaration())), JLabel.LEFT);
        algorithmInfo.setToolTipText(TooltipUtils.getAlgorithmTooltip(testbench.getTargetAlgorithm().getDeclaration()));
        toolBar.add(algorithmInfo);

        toolBar.add(Box.createHorizontalGlue());

        backupSelection = new JComboBox<>();
        backupSelection.setRenderer(new ACAQDataSlotBackupListCellRenderer());
        backupSelection.addActionListener(e -> {
            if (backupSelection.getSelectedItem() != null)
                loadBackup((ACAQAlgorithmBackup) backupSelection.getSelectedItem());
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
        if (backupSelection.getSelectedItem() instanceof ACAQAlgorithmBackup) {
            ACAQAlgorithmBackup backup = (ACAQAlgorithmBackup) backupSelection.getSelectedItem();
            backup.restoreParameters(testbench.getTargetAlgorithm());
            testbench.getTargetAlgorithm().getEventBus().post(new ReloadSettingsRequestedEvent(testbench.getTargetAlgorithm()));
            getWorkbenchUI().sendStatusBarText("Copied parameters from testbench to " + testbench.getTargetAlgorithm().getName());
        }
    }

    private void renameCurrentBackup() {
        if (backupSelection.getSelectedItem() instanceof ACAQAlgorithmBackup) {
            ACAQAlgorithmBackup backup = (ACAQAlgorithmBackup) backupSelection.getSelectedItem();
            String newName = JOptionPane.showInputDialog(this, "Please enter a label", backup.getLabel());
            backup.setLabel(newName);
            updateBackupSelection();
        }
    }

    private void clearBackups() {
        Object current = backupSelection.getSelectedItem();
        testbench.getBackupList().removeIf(b -> b != current);
        updateBackupSelection();
    }

    private void updateBackupSelection() {
        Object currentSelection = backupSelection.getSelectedItem();
        DefaultComboBoxModel<ACAQAlgorithmBackup> model = new DefaultComboBoxModel<>(testbench.getBackupList().toArray(new ACAQAlgorithmBackup[0]));
        if (currentSelection != null && testbench.getBackupList().contains(currentSelection))
            model.setSelectedItem(currentSelection);
        backupSelection.setModel(model);
    }

    private void newTest() {

        // Check if we are still valid
        ACAQValidityReport report = new ACAQValidityReport();
        testbench.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report);
            return;
        }

        testbench.newTest();

        newTestButton.setEnabled(false);
        ACAQRunnerQueue.getInstance().enqueue(testbench);
    }

    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == testbench) {
            newTestButton.setEnabled(true);
            testbench.createBackup();
            updateBackupSelection();
            backupSelection.setSelectedItem(testbench.getLatestBackup());
        }
    }

    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getRun() == testbench) {
            newTestButton.setEnabled(true);
            UIUtils.openErrorDialog(this, event.getException());
        }
    }

    private void loadBackup(ACAQAlgorithmBackup backup) {

        int dividerLocation = splitPane.getDividerLocation();

        backup.restore(testbench.getBenchedAlgorithm());
        ACAQParameterAccessUI parameters = new ACAQParameterAccessUI(getWorkbenchUI(), testbench.getBenchedAlgorithm(),
                MarkdownDocument.fromPluginResource("documentation/testbench.md"),
                true, true);
        splitPane.setLeftComponent(parameters);

        ACAQResultUI resultUI = new ACAQResultUI(getWorkbenchUI(), testbench.getTestbenchRun());
        splitPane.setRightComponent(resultUI);

        revalidate();
        repaint();

        splitPane.setDividerLocation(dividerLocation);
    }
}
