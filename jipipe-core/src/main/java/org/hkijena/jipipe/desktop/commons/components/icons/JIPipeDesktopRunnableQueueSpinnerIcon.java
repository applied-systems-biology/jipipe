/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components.icons;

import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;

import java.awt.*;

public class JIPipeDesktopRunnableQueueSpinnerIcon extends SpinnerIcon implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.StartedEventListener, JIPipeRunnable.InterruptedEventListener {

    public JIPipeDesktopRunnableQueueSpinnerIcon(Component parent) {
        this(parent, JIPipeQueuedRunnableExecutor.getInstance());
    }

    public JIPipeDesktopRunnableQueueSpinnerIcon(Component parent, JIPipeRunnableExecutor runnerQueue) {
        this(parent, runnerQueue, 16);
    }

    public JIPipeDesktopRunnableQueueSpinnerIcon(Component parent, JIPipeRunnableExecutor runnerQueue, int size) {
        super(parent, size);

        runnerQueue.getFinishedEventEmitter().subscribeWeak(this);
        runnerQueue.getStartedEventEmitter().subscribeWeak(this);
        runnerQueue.getInterruptedEventEmitter().subscribeWeak(this);
        if (!JIPipeQueuedRunnableExecutor.getInstance().isEmpty()) {
            start();
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (JIPipeQueuedRunnableExecutor.getInstance().isEmpty()) {
            stop();
        }
    }

    @Override
    public void onRunnableStarted(JIPipeRunnable.StartedEvent event) {
        start();
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (JIPipeQueuedRunnableExecutor.getInstance().isEmpty()) {
            stop();
        }
    }
}
