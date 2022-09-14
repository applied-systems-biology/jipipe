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
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerInterruptedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerStartedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class JIPipeRunThrobberIcon extends NewThrobberIcon {

    public JIPipeRunThrobberIcon(Component parent) {
        super(parent);

        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
        if (!JIPipeRunnerQueue.getInstance().isEmpty()) {
            start();
        }
    }

    @Subscribe
    public void onWorkerFinished(RunWorkerFinishedEvent event) {
        if (JIPipeRunnerQueue.getInstance().isEmpty()) {
            stop();
        }
    }

    @Subscribe
    public void onWorkerStart(RunWorkerStartedEvent event) {
        start();
    }

    @Subscribe
    public void onWorkerInterrupted(RunWorkerInterruptedEvent event) {
        if (JIPipeRunnerQueue.getInstance().isEmpty()) {
            stop();
        }
    }
}
