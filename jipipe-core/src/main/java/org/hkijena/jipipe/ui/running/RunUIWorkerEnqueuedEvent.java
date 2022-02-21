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

package org.hkijena.jipipe.ui.running;

import org.hkijena.jipipe.api.JIPipeRunnable;

/**
 * Generated when an {@link JIPipeRunWorker} was enqueued
 */
public class RunUIWorkerEnqueuedEvent {

    private JIPipeRunnable run;
    private JIPipeRunWorker worker;

    /**
     * @param run    the run
     * @param worker the worker
     */
    public RunUIWorkerEnqueuedEvent(JIPipeRunnable run, JIPipeRunWorker worker) {
        this.run = run;
        this.worker = worker;
    }

    public JIPipeRunWorker getWorker() {
        return worker;
    }

    public JIPipeRunnable getRun() {
        return run;
    }
}