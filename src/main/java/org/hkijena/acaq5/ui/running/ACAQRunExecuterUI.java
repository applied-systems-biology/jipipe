package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.events.RunFinishedEvent;
import org.hkijena.acaq5.api.events.RunInterruptedEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class ACAQRunExecuterUI extends JPanel {
    private ACAQRun run;
    private JProgressBar progressBar;
    private JButton cancelButton;
    private Worker worker;
    private EventBus eventBus = new EventBus();

    public ACAQRunExecuterUI(ACAQRun run) {
        this.run = run;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0,8,8,8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        progressBar = new JProgressBar();
        progressBar.setString("Ready");
        progressBar.setStringPainted(true);
        buttonPanel.add(progressBar);
        buttonPanel.add(Box.createHorizontalStrut(16));

        cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> requestCancelRun());
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.CENTER);
    }

    public void startRun() {
        worker = new Worker(run, progressBar);
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
                                SwingUtilities.invokeLater(() -> postInterruptedEvent(new RuntimeException("Execution was cancelled by user!")));
                            }
                            else if(worker.get() != null) {
                                final Exception e = worker.get();
                                SwingUtilities.invokeLater(() -> postInterruptedEvent(e));
                            }
                            else {
                                postFinishedEvent();
                            }
                        } catch (InterruptedException | ExecutionException | CancellationException e) {
                            SwingUtilities.invokeLater(() -> postInterruptedEvent(e));
                        }
                        break;
                }
            }
        });
        worker.execute();
    }

    private void postFinishedEvent() {
        eventBus.post(new RunFinishedEvent(run));
    }

    private void postInterruptedEvent(Exception e) {
        eventBus.post(new RunInterruptedEvent(run, e));
    }

    public void requestCancelRun() {
        cancelButton.setEnabled(false);
        worker.cancel(true);
    }

    public EventBus getEventBus() {
        return eventBus;
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
