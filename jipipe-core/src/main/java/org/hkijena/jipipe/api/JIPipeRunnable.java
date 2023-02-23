/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.ui.running.*;

import java.util.function.Consumer;

/**
 * Runnable that can be scheduled, canceled, and reports progress
 */
public interface JIPipeRunnable extends Runnable {

    /**
     * The info object that allows communication with the run
     *
     * @return the info object
     */
    JIPipeProgressInfo getProgressInfo();

    /**
     * Sets the progress info of this runnable
     *
     * @param progressInfo the info object
     */
    void setProgressInfo(JIPipeProgressInfo progressInfo);

    /**
     * A name for the runnable
     *
     * @return the name
     */
    String getTaskLabel();

    /**
     * Registers an event subscriber into {@link org.hkijena.jipipe.ui.running.JIPipeRunnerQueue} that listens for when
     * this run finishes
     *
     * @param function the method to execute when the run finishes (successfully or not successfully)
     */
    default void onFinished(Consumer<RunWorkerFinishedEvent> function) {
        JIPipeRunnerQueue.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerFinished(RunWorkerFinishedEvent event) {
                if (event.getRun() == JIPipeRunnable.this) {
                    function.accept(event);
                }
            }
        });
    }

    /**
     * Registers an event subscriber into {@link org.hkijena.jipipe.ui.running.JIPipeRunnerQueue} that listens for when
     * this run is interrupted
     *
     * @param function the method to execute when the run is interrupted (due to error or user cancel)
     */
    default void onInterrupted(Consumer<RunWorkerInterruptedEvent> function) {
        JIPipeRunnerQueue.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerInterrupted(RunWorkerInterruptedEvent event) {
                if (event.getRun() == JIPipeRunnable.this) {
                    function.accept(event);
                }
            }
        });
    }

    /**
     * Registers an event subscriber into {@link org.hkijena.jipipe.ui.running.JIPipeRunnerQueue} that listens for when
     * this run is started
     *
     * @param function the method to execute when the run starts
     */
    default void onStarted(Consumer<RunWorkerStartedEvent> function) {
        JIPipeRunnerQueue.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerStarted(RunWorkerStartedEvent event) {
                if (event.getRun() == JIPipeRunnable.this) {
                    function.accept(event);
                }
            }
        });
    }

    /**
     * Registers an event subscriber into {@link org.hkijena.jipipe.ui.running.JIPipeRunnerQueue} that listens for when
     * this run is progressing
     *
     * @param function the method to execute when the run progresses
     */
    default void onProgress(Consumer<RunWorkerProgressEvent> function) {
        JIPipeRunnerQueue.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void onWorkerStarted(RunWorkerProgressEvent event) {
                if (event.getRun() == JIPipeRunnable.this) {
                    function.accept(event);
                }
            }
        });
    }
}
