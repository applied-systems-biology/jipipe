package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.MutableACAQRunConfiguration;
import org.hkijena.acaq5.api.events.RunFinishedEvent;
import org.hkijena.acaq5.api.events.RunInterruptedEvent;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class ACAQRunSettingsUI extends ACAQUIPanel {

    private ACAQRun run;

    public ACAQRunSettingsUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        try {
            run = new ACAQRun(getWorkbenchUI().getProject(), new MutableACAQRunConfiguration());
            initializeSetupGUI();
        }
        catch(Exception e) {
            openError(e);
        }
    }

    private void initializeSetupGUI() {
        JPanel setupPanel = new JPanel(new BorderLayout());
        ACAQParameterAccessUI formPanel = new ACAQParameterAccessUI(run.getConfiguration(),
                "documentation/run.md",
                false);

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
    }

    private void runNow() {
        removeAll();
        ACAQRunExecuterUI executerUI = new ACAQRunExecuterUI(run);
        add(executerUI, BorderLayout.SOUTH);
        revalidate();
        repaint();
        executerUI.getEventBus().register(this);
        executerUI.startRun();
    }

    @Subscribe
    public void onRunFinished(RunFinishedEvent event) {
        openResults();
    }

    @Subscribe
    public void onRunInterrupted(RunInterruptedEvent event) {
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
