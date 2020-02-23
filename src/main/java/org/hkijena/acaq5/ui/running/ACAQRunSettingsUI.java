package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.ACAQMutableRunConfiguration;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.*;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ACAQRunSettingsUI extends ACAQUIPanel {

    private ACAQRun run;

    public ACAQRunSettingsUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        initialize();
        ACAQRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        ACAQValidityReport report = new ACAQValidityReport();
        getWorkbenchUI().getProject().reportValidity(report);
        if(report.isValid()) {
            initializeSetupGUI();
        }
        else {
            initializeValidityCheckUI(report);
        }
    }

    private void initializeValidityCheckUI(ACAQValidityReport report) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(8,8));
        ACAQValidityReportUI reportUI = new ACAQValidityReportUI();
        reportUI.setReport(report);

        MarkdownReader help = new MarkdownReader(false);
        help.loadDefaultDocument("documentation/validation.md");

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
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0,8,8,8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton runButton = new JButton("Retry", UIUtils.getIconFromResources("refresh.png"));
        runButton.addActionListener(e -> {
            report.clear();
            getWorkbenchUI().getProject().reportValidity(report);
            getWorkbenchUI().sendStatusBarText("Re-validated ACAQ5 project");
            if(report.isValid())
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
            run = new ACAQRun(getWorkbenchUI().getProject(), new ACAQMutableRunConfiguration());
        }
        catch(Exception e) {
            openError(e);
            return;
        }

        removeAll();
        JPanel setupPanel = new JPanel(new BorderLayout());
        ACAQParameterAccessUI formPanel = new ACAQParameterAccessUI(run.getConfiguration(),
                "documentation/run.md",
                false,
                true);

        setupPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0,8,8,8));
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

    @Subscribe
    public void onRunFinished(RunUIWorkerFinishedEvent event) {
        if(event.getRun() == run)
            openResults();
    }

    @Subscribe
    public void onRunInterrupted(RunUIWorkerInterruptedEvent event) {
        if(event.getRun() == run)
            openError(event.getException());
    }

    private void openError(Exception exception) {
        removeAll();
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        JTextArea errorPanel = new JTextArea(writer.toString());
        errorPanel.setEditable(false);
        add(new JScrollPane(errorPanel), BorderLayout.CENTER);
        revalidate();
    }

    private void openResults() {
        ACAQResultUI resultUI = new ACAQResultUI(getWorkbenchUI(), run);
        removeAll();
        add(resultUI, BorderLayout.CENTER);
        revalidate();
    }
}
