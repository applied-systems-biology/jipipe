package org.hkijena.jipipe.desktop.app.running.queue;

import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Unified component that displays the status of a {@link JIPipeRunnableQueue}
 */
public class JIPipeDesktopRunQueueLoggerPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.ProgressEventListener, JIPipeRunnable.FinishedEventListener {

    private final JIPipeRunnableQueue queue;

    private final JLabel statusLabel = new JLabel("Ready ...");
    private final JProgressBar progressBar = new JProgressBar();
    private JIPipeRunnable targetRun;

    public JIPipeDesktopRunQueueLoggerPanel(JIPipeDesktopWorkbench desktopWorkbench, JIPipeRunnableQueue queue) {
        super(desktopWorkbench);
        this.queue = queue;
        initialize();

        queue.getStartedEventEmitter().subscribeWeak(this);
        queue.getInterruptedEventEmitter().subscribeWeak(this);
        queue.getProgressEventEmitter().subscribeWeak(this);
        queue.getFinishedEventEmitter().subscribeWeak(this);

        // TODO: load existing log
        if(queue.getCurrentRun() != null) {

        }
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        
    }

    public void addDefaultCancelButton() {

    }

    public void addButton(JButton button) {

    }

    public JIPipeRunnable getTargetRun() {
        return targetRun;
    }

    public void setTargetRun(JIPipeRunnable targetRun) {
        this.targetRun = targetRun;
    }

    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
//        if (event.getRun() == run) {
//            progressBar.setIndeterminate(false);
//            progressBar.setMaximum(event.getStatus().getMaxProgress());
//            progressBar.setValue(event.getStatus().getProgress());
//            progressBar.setString("(" + progressBar.getValue() + "/" + progressBar.getMaximum() + ") " + event.getStatus().getMessage());
//            log.append(event.getStatus().render() + "\n");
//        } else {
//            log.append("[~] " + event.getStatus().render() + "\n");
//        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
//        if (event.getRun() == run) {
//            switchToCloseButtonIfPossible();
//            progressBar.setString("Finished");
//        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {

    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {

    }
}
