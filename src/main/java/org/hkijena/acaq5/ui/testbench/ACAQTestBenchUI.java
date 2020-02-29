package org.hkijena.acaq5.ui.testbench;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQMutableRunConfiguration;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.testbench.ACAQAlgorithmBackup;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.ConfirmingButton;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.events.ReloadSettingsRequestedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueue;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ACAQTestBenchUI extends ACAQUIPanel {

    private ACAQAlgorithm projectAlgorithm;
    private ACAQRun run;
    private ACAQAlgorithm runAlgorithm;
    private List<ACAQAlgorithmBackup> backupList = new ArrayList<>();
    private JComboBox<ACAQAlgorithmBackup> backupSelection;
    private JButton newTestButton;
    private JSplitPane splitPane;

    public ACAQTestBenchUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithm projectAlgorithm, ACAQRun run) {
        super(workbenchUI);
        this.projectAlgorithm = projectAlgorithm;
        this.run = run;
        this.runAlgorithm = run.getGraph().getAlgorithmNodes().get(projectAlgorithm.getIdInGraph());

        // Do the initial backup
        backupList.add(new ACAQAlgorithmBackup(runAlgorithm));

        // Force to only run the end algorithm
        ((ACAQMutableRunConfiguration) run.getConfiguration()).setOnlyRunningEndAlgorithm(true);

        initialize();
        updateBackupSelection();
        loadBackup(backupList.get(0));

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

        String compartmentName = getProject().getCompartments().get(projectAlgorithm.getCompartment()).getName();
        JLabel compartmentInfo = new JLabel(compartmentName, UIUtils.getIconFromResources("graph-compartment.png"), JLabel.LEFT);
        toolBar.add(compartmentInfo);

        toolBar.add(Box.createHorizontalStrut(8));

        JLabel algorithmInfo = new JLabel(projectAlgorithm.getName(), new ColorIcon(16, 16,
                UIUtils.getFillColorFor(projectAlgorithm.getDeclaration())), JLabel.LEFT);
        algorithmInfo.setToolTipText(TooltipUtils.getAlgorithmTooltip(projectAlgorithm.getDeclaration()));
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
            backup.restoreParameters(projectAlgorithm);
            projectAlgorithm.getEventBus().post(new ReloadSettingsRequestedEvent(projectAlgorithm));
            getWorkbenchUI().sendStatusBarText("Copied parameters from testbench to " + projectAlgorithm.getName());
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
        backupList.removeIf(b -> b != current);
        updateBackupSelection();
    }

    private void updateBackupSelection() {
        Object currentSelection = backupSelection.getSelectedItem();
        DefaultComboBoxModel<ACAQAlgorithmBackup> model = new DefaultComboBoxModel<>(backupList.toArray(new ACAQAlgorithmBackup[0]));
        if (currentSelection != null && backupList.contains(currentSelection))
            model.setSelectedItem(currentSelection);
        backupSelection.setModel(model);
    }

    private void newTest() {

        // Check if we are still valid
        ACAQValidityReport report = new ACAQValidityReport();
        runAlgorithm.reportValidity(report);
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report);
            return;
        }

        newTestButton.setEnabled(false);
        Path outputBasePath = run.getConfiguration().getOutputPath().getParent();
        Path outputPath;
        int index = 1;
        do {
            outputPath = outputBasePath.resolve("test-" + index);
            ++index;
        }
        while (Files.isDirectory(outputPath));
        ((ACAQMutableRunConfiguration) run.getConfiguration()).setOutputPath(outputPath);

        ACAQRunnerQueue.getInstance().enqueue(run);
    }

    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == run) {
            newTestButton.setEnabled(true);
            backupList.add(new ACAQAlgorithmBackup(runAlgorithm));
            updateBackupSelection();
            backupSelection.setSelectedItem(backupList.get(backupList.size() - 1));
        }
    }

    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getRun() == run) {
            newTestButton.setEnabled(true);
            UIUtils.openErrorDialog(this, event.getException());
        }
    }

    private void loadBackup(ACAQAlgorithmBackup backup) {

        int dividerLocation = splitPane.getDividerLocation();

        backup.restore(runAlgorithm);
        ACAQParameterAccessUI parameters = new ACAQParameterAccessUI(runAlgorithm,
                MarkdownDocument.fromPluginResource("documentation/testbench.md"),
                true, true);
        splitPane.setLeftComponent(parameters);

        ACAQTestBenchResultUI resultUI = new ACAQTestBenchResultUI(getWorkbenchUI(), runAlgorithm);
        splitPane.setRightComponent(resultUI);

        revalidate();
        repaint();

        splitPane.setDividerLocation(dividerLocation);
    }
}
