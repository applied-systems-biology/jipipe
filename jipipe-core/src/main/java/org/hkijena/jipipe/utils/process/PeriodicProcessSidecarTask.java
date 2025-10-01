package org.hkijena.jipipe.utils.process;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A sidecar task that runs every x milliseconds
 */
public abstract class PeriodicProcessSidecarTask implements ProcessSidecarTask {

    private final long period;
    private Timer timer;

    public PeriodicProcessSidecarTask(long period) {
        this.period = period;
    }

    @Override
    public void start(ExtendedExecutor executor) throws Exception {
        if (timer != null) {
            throw new IllegalStateException("Timer has already been started");
        }
        timer = new Timer(true);
        timer.schedule(new  TimerTask() {
            @Override
            public void run() {
                tick(executor);
            }
        }, period, period);
    }

    protected abstract void tick(ExtendedExecutor executor);

    @Override
    public void stop() {
        if(timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }
}
