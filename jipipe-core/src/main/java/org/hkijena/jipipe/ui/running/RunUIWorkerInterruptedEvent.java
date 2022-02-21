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
 * Generated when work of an {@link JIPipeRunWorker} is interrupted
 */
public class RunUIWorkerInterruptedEvent {

    private Throwable exception;
    private JIPipeRunWorker worker;

    /**
     * @param worker    the worker
     * @param exception the exception triggered when interrupted
     */
    public RunUIWorkerInterruptedEvent(JIPipeRunWorker worker, Throwable exception) {
        this.exception = exception;
        this.worker = worker;
    }

    public JIPipeRunWorker getWorker() {
        return worker;
    }

    public Throwable getException() {
        return exception;
    }

    public JIPipeRunnable getRun() {
        return worker.getRun();
    }
}