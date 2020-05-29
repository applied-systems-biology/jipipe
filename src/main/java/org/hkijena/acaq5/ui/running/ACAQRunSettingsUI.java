package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQMutableRunConfiguration;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.*;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Settings UI for {@link org.hkijena.acaq5.api.ACAQRunConfiguration}
 */
public class ACAQRunSettingsUI extends ACAQProjectWorkbenchPanel {

    private ACAQRun run;

    /**
     * @param workbenchUI workbench UI
     */
    public ACAQRunSettingsUI(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        ACAQRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        ACAQValidityReport report = new ACAQValidityReport();
        getProjectWorkbench().getProject().reportValidity(report);
        if (report.isValid()) {
            initializeSetupGUI();
        } else {
            initializeValidityCheckUI(report);
        }
    }

    private void initializeValidityCheckUI(ACAQValidityReport report) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(8, 8));
        ACAQValidityReportUI reportUI = new ACAQValidityReportUI(false);
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

        JButton runButton = new JButton("Retry", UIUtils.getIconFromResources("refresh.png"));
        runButton.addActionListener(e -> {
            report.clear();
            getProjectWorkbench().getProject().reportValidity(report);
            getProjectWorkbench().sendStatusBarText("Re-validated ACAQ5 project");
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
            run = new ACAQRun(getProjectWorkbench().getProject(), new ACAQMutableRunConfiguration());
        } catch (Exception e) {
            openError(e);
            return;
        }

        removeAll();
        JPanel setupPanel = new JPanel(new BorderLayout());
        ParameterPanel formPanel = new ParameterPanel(getProjectWorkbench(), run.getConfiguration(),
                MarkdownDocument.fromPluginResource("documentation/run.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);

        Set<ACAQAlgorithm> algorithmsWithMissingInput = getProjectWorkbench().getProject().getGraph().getAlgorithmsWithMissingInput();
        if (!algorithmsWithMissingInput.isEmpty()) {
            formPanel.removeLastRow();
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("Unexecuted algorithms", UIUtils.getIconFromResources("warning.png"));
            headerPanel.getDescriptionArea().setVisible(true);
            headerPanel.getDescriptionArea().setText("There are algorithms that will not be executed, as they are missing input data. " +
                    "If this is not intended, please check if the listed algorithms have all input slots connected.");

            DefaultTableModel model = new DefaultTableModel();
            model.setColumnIdentifiers(new Object[]{"Compartment", "Algorithm name"});
            for (ACAQAlgorithm algorithm : algorithmsWithMissingInput.stream().sorted(Comparator.comparing(ACAQAlgorithm::getCompartment)).collect(Collectors.toList())) {
                model.addRow(new Object[]{
                        StringUtils.createIconTextHTMLTable(getProjectWorkbench().getProject().getCompartments().get(algorithm.getCompartment()).getName(),
                                ResourceUtils.getPluginResource("icons/graph-compartment.png")),
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

        setupPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton runButton = new JButton("Run now", UIUtils.getIconFromResources("run.png"));
        runButton.addActionListener(e -> runNow());
        buttonPanel.add(runButton);

        add(buttonPanel, BorderLayout.SOUTH);

        add(setupPanel, BorderLayout.CENTER);
        revalidate();
    }

    private void runNow() {
        removeAll();
        ACAQRunExecuterUI executerUI = new ACAQRunExecuterUI(run);
        add(executerUI, BorderLayout.SOUTH);
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
        revalidate();
    }

    private void openResults() {
        ACAQResultUI resultUI = new ACAQResultUI(getProjectWorkbench(), run);
        removeAll();
        add(resultUI, BorderLayout.CENTER);
        revalidate();
    }
}
