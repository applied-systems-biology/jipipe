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

package org.hkijena.jipipe.ui.components.icons;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;

import java.awt.*;

public class JIPipeRunnerQueueThrobberIcon extends NewThrobberIcon {

    public JIPipeRunnerQueueThrobberIcon(Component parent) {
        this(parent, JIPipeRunnerQueue.getInstance());
    }

    public JIPipeRunnerQueueThrobberIcon(Component parent, JIPipeRunnerQueue runnerQueue) {
        super(parent);

        runnerQueue.getEventBus().register(this);
        if (!JIPipeRunnerQueue.getInstance().isEmpty()) {
            start();
        }
    }

    @Subscribe
    public void onWorkerFinished(JIPipeRunnable.FinishedEvent event) {
        if (JIPipeRunnerQueue.getInstance().isEmpty()) {
            stop();
        }
    }

    @Subscribe
    public void onWorkerStart(JIPipeRunnable.StartedEvent event) {
        start();
    }

    @Subscribe
    public void onWorkerInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (JIPipeRunnerQueue.getInstance().isEmpty()) {
            stop();
        }
    }
}
