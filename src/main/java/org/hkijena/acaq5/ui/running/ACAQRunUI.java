package org.hkijena.acaq5.ui.running;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
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

public class ACAQRunUI extends ACAQUIPanel {

    private ACAQRun run;
//    private ACAQValidityReport validityReport;
    private Worker worker;

    JPanel setupPanel;
    JButton cancelButton;
    JButton runButton;
    JProgressBar progressBar;
    JPanel buttonPanel;

    public ACAQRunUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        run = new ACAQRun(workbenchUI.getProject());
//        validityReport = run.getValidityReport();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        initializeButtons();
        initializeSetupGUI();
    }

    private void initializeSetupGUI() {
        setupPanel = new JPanel(new BorderLayout());
        FormPanel formPanel = new FormPanel("documentation/run.md", false);

        FileSelection outputFolderSelection = formPanel.addToForm(new FileSelection(FileSelection.Mode.OPEN),
                new JLabel("Output folder"),
                null);
        outputFolderSelection.getFileChooser().setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        outputFolderSelection.addActionListener(e -> {
            run.setOutputPath(outputFolderSelection.getPath());
            runButton.setEnabled(outputFolderSelection.getPath() != null);
        });
        formPanel.addVerticalGlue();

        setupPanel.add(formPanel, BorderLayout.CENTER);
        add(setupPanel, BorderLayout.CENTER);
    }

    private void initializeButtons() {
        buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0,8,8,8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        progressBar = new JProgressBar();
        progressBar.setString("Ready");
        progressBar.setStringPainted(true);
        buttonPanel.add(progressBar);
        buttonPanel.add(Box.createHorizontalStrut(16));

        cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        buttonPanel.add(cancelButton);

        runButton = new JButton("Run now", UIUtils.getIconFromResources("run.png"));
        runButton.addActionListener(e -> runNow());
        runButton.setEnabled(false);
        buttonPanel.add(runButton);

        add(buttonPanel, BorderLayout.SOUTH);

        cancelButton.setVisible(false);
    }

    private void runNow() {
        runButton.setVisible(false);
        cancelButton.setVisible(true);
        setupPanel.setVisible(false);
        remove(setupPanel);

        Worker worker = new Worker(run, progressBar);
        cancelButton.addActionListener(e -> {
            cancelButton.setEnabled(false);
            worker.cancel(true);
        });
        worker.addPropertyChangeListener(p -> {
            if("state".equals(p.getPropertyName())) {
                switch ((SwingWorker.StateValue)p.getNewValue()) {
                    case DONE:
                        cancelButton.setEnabled(false);
                        if(!worker.isCancelled()) {
                            cancelButton.setVisible(false);
                        }
                        try {
                            if(worker.isCancelled()) {
                                SwingUtilities.invokeLater(() -> openError(new RuntimeException("Execution was cancelled by user!")));
                            }
                            else if(worker.get() != null) {
                                final Exception e = worker.get();
                                SwingUtilities.invokeLater(() -> openError(e));
                            }
                            else {
                                openResults();
                            }
                        } catch (InterruptedException | ExecutionException | CancellationException e) {
                            SwingUtilities.invokeLater(() -> openError(e));
                        }
                        break;
                }
            }
        });
        worker.execute();
    }

    private void openError(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        JTextArea errorPanel = new JTextArea(writer.toString());
        errorPanel.setEditable(false);
        add(new JScrollPane(errorPanel), BorderLayout.CENTER);
        revalidate();
    }

    private void openResults() {
        ACAQResultUI resultUI = new ACAQResultUI(run);
        add(resultUI, BorderLayout.CENTER);
        buttonPanel.setVisible(false);
        revalidate();
    }

    private static class Worker extends SwingWorker<Exception, Object> {

        private JProgressBar progressBar;
        private ACAQRun run;

        public Worker(ACAQRun run, JProgressBar progressBar) {
            this.progressBar = progressBar;
            this.run = run;

            progressBar.setMaximum(run.getGraph().getAlgorithmCount());
            progressBar.setValue(0);
        }

        private void onStatus(ACAQRun.Status status) {
            publish(status.getCurrentTask());
            publish(status.getProgress());
        }

        @Override
        protected Exception doInBackground() throws Exception {
            try {
                run.run(this::onStatus, this::isCancelled);
            }
            catch(Exception e) {
                e.printStackTrace();
                return e;
            }

            return null;
        }

        @Override
        protected void process(List<Object> chunks) {
            super.process(chunks);
            for(Object chunk : chunks) {
                if(chunk instanceof Integer) {
                    progressBar.setValue((Integer)chunk);
                }
                else if(chunk instanceof String) {
                    progressBar.setString("(" + progressBar.getValue() + "/" + progressBar.getMaximum() + ") " + chunk);
                }
            }
        }

        @Override
        protected void done() {
            progressBar.setString("Finished.");
        }
    }
}
