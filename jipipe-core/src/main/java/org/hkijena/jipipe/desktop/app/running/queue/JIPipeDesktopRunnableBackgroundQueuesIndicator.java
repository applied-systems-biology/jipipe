package org.hkijena.jipipe.desktop.app.running.queue;

import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel that handles multiple queues and displays a flashing icon while a background process is currently running
 */
public class JIPipeDesktopRunnableBackgroundQueuesIndicator extends JPanel implements JIPipeRunnable.StartedEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private final List<Entry> queues = new ArrayList<>();
    private final SpinnerIcon spinnerIcon;
    private final StaticDebouncer debouncer;

    public JIPipeDesktopRunnableBackgroundQueuesIndicator() {
        this.spinnerIcon = new SpinnerIcon(this);
        this.debouncer = new StaticDebouncer(250, this::rebuild);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
    }

    public void addQueue(JIPipeRunnableExecutor queue, Icon icon) {
        if (queues.stream().anyMatch(e -> e.queue == queue)) {
            return;
        }
        queues.add(new Entry(queue, icon));
        queue.getStartedEventEmitter().subscribeWeak(this);
        queue.getFinishedEventEmitter().subscribeWeak(this);
        queue.getInterruptedEventEmitter().subscribeWeak(this);

        rebuild();
    }

    private void rebuild() {
        removeAll();

        boolean anyRunning = false;
        for (Entry entry : queues) {
            if (!entry.queue.isEmpty()) {
                anyRunning = true;
                break;
            }
        }
        if (anyRunning) {
            spinnerIcon.start();
        } else {
            spinnerIcon.stop();
        }

        for (Entry entry : queues) {
            if (!entry.queue.isEmpty()) {
                add(createIconLabel(entry.icon));
            }
        }
        if (anyRunning) {
            add(createIconLabel(spinnerIcon));
        }

        revalidate();
        repaint();
    }

    private JLabel createIconLabel(Icon icon) {
        JLabel label = new JLabel(icon);
        label.setBorder(UIUtils.createEmptyBorder(2));
        return label;
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        rebuild();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        rebuild();
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        rebuild();
    }

    private record Entry(JIPipeRunnableExecutor queue, Icon icon) {

    }
}
