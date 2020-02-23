package org.hkijena.acaq5.ui.testbench;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQRunSample;
import org.hkijena.acaq5.api.ACAQMutableRunConfiguration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueue;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class ACAQTestBenchUI extends ACAQUIPanel {

    private ACAQAlgorithm projectAlgorithm;
    private ACAQRun run;
    private String sampleName;
    private ACAQAlgorithm runAlgorithm;
    private List<ACAQAlgorithmBackup> backupList = new ArrayList<>();
    private JComboBox<ACAQAlgorithmBackup> backupSelection;
    private JButton newTestButton;
    private JSplitPane splitPane;

    public ACAQTestBenchUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithm projectAlgorithm, ACAQRun run) {
        super(workbenchUI);
        this.projectAlgorithm = projectAlgorithm;
        this.run = run;

        // Associate sample -> appropriate run algorithm
        for (Map.Entry<String, ACAQRunSample> entry : run.getSamples().entrySet()) {
            ACAQAlgorithm algorithm = entry.getValue().getAlgorithms().stream().filter(a -> run.getProjectAlgorithms().get(a) == projectAlgorithm).findFirst().orElse(null);
            if(algorithm != null) {
                sampleName = entry.getKey();
                runAlgorithm = algorithm;
                break;
            }
        }

        if(runAlgorithm == null)
            throw new IllegalArgumentException("The provided testbench parameters should be associated to exactly one sample!");

        // Do the initial backup
        backupList.add(new ACAQAlgorithmBackup(runAlgorithm));

        // Force to only run the end algorithm
        ((ACAQMutableRunConfiguration)run.getConfiguration()).setOnlyRunningEndAlgorithm(true);

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

        JLabel sampleInfo = new JLabel(sampleName, UIUtils.getIconFromResources("sample.png"), JLabel.LEFT);
        toolBar.add(sampleInfo);

        toolBar.add(Box.createHorizontalStrut(8));

        JLabel algorithmInfo = new JLabel(projectAlgorithm.getName(),  new ColorIcon(16, 16,
                projectAlgorithm.getCategory().getColor(0.1f, 0.9f)), JLabel.LEFT);
        algorithmInfo.setToolTipText(TooltipUtils.getAlgorithmTooltip(projectAlgorithm.getClass()));
        toolBar.add(algorithmInfo);

        toolBar.add(Box.createHorizontalGlue());

        backupSelection = new JComboBox<>();
        backupSelection.setRenderer(new ACAQDataSlotBackupListCellRenderer());
        backupSelection.addActionListener(e -> {
            if(backupSelection.getSelectedItem() != null)
                loadBackup((ACAQAlgorithmBackup) backupSelection.getSelectedItem());
        });
        toolBar.add(backupSelection);

        JButton clearBackupsButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        clearBackupsButton.setToolTipText("Remove all other time points");
        clearBackupsButton.addActionListener(e -> clearBackups());
        toolBar.add(clearBackupsButton);

        toolBar.add(Box.createHorizontalStrut(8));

        newTestButton = new JButton("New test", UIUtils.getIconFromResources("run.png"));
        newTestButton.addActionListener(e -> newTest());
        toolBar.add(newTestButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void clearBackups() {
        ACAQAlgorithmBackup current = (ACAQAlgorithmBackup) backupSelection.getSelectedItem();
        backupList.removeIf(b -> b != current);
        updateBackupSelection();
    }

    private void updateBackupSelection() {
        Object currentSelection = backupSelection.getSelectedItem();
        DefaultComboBoxModel<ACAQAlgorithmBackup> model = new DefaultComboBoxModel<>(backupList.toArray(new ACAQAlgorithmBackup[0]));
        if(currentSelection != null && backupList.contains(currentSelection))
            model.setSelectedItem(currentSelection);
        backupSelection.setModel(model);
    }

    private void newTest() {
        newTestButton.setEnabled(false);
        Path outputBasePath = run.getConfiguration().getOutputPath().getParent();
        Path outputPath;
        int index = 1;
        do {
            outputPath = outputBasePath.resolve("test-" + index);
            ++index;
        }
        while(Files.isDirectory(outputPath));
        ((ACAQMutableRunConfiguration)run.getConfiguration()).setOutputPath(outputPath);

        ACAQRunnerQueue.getInstance().enqueue(run);
    }

    @Subscribe
    public void onWorkerFinished(RunUIWorkerFinishedEvent event) {
        if(event.getRun() == run) {
            newTestButton.setEnabled(true);
            backupList.add(new ACAQAlgorithmBackup(runAlgorithm));
            updateBackupSelection();
            backupSelection.setSelectedItem(backupList.get(backupList.size() - 1));
        }
    }

    @Subscribe
    public void onWorkerInterrupted(RunUIWorkerInterruptedEvent event) {
        if(event.getRun() == run) {
            newTestButton.setEnabled(true);
            openError(event.getException());
        }
    }

    private void loadBackup(ACAQAlgorithmBackup backup) {

        backup.restore(runAlgorithm);
        ACAQParameterAccessUI parameters = new ACAQParameterAccessUI(runAlgorithm, "documentation/testbench.md",
                true, true);
        splitPane.setLeftComponent(parameters);

        ACAQTestBenchResultUI resultUI = new ACAQTestBenchResultUI(getWorkbenchUI(), runAlgorithm, run.getSamples().get(sampleName));
        splitPane.setRightComponent(resultUI);

        revalidate();
        repaint();

        splitPane.setDividerLocation(0.33);
    }

    private void openError(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        JTextArea errorArea = new JTextArea(writer.toString());
        errorArea.setEditable(false);

        JDialog dialog = new JDialog();
        dialog.setTitle("Error");
        dialog.setContentPane(errorArea);
        dialog.setModal(false);
        dialog.pack();
        dialog.setSize(new Dimension(500,400));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
