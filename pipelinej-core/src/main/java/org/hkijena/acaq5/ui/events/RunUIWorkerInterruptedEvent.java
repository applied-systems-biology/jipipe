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

package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

/**
 * Generated when work of an {@link ACAQRunWorker} is interrupted
 */
public class RunUIWorkerInterruptedEvent {

    private Exception exception;
    private ACAQRunWorker worker;

    /**
     * @param worker    the worker
     * @param exception the exception triggered when interrupted
     */
    public RunUIWorkerInterruptedEvent(ACAQRunWorker worker, Exception exception) {
        this.exception = exception;
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public Exception getException() {
        return exception;
    }

    public ACAQRunnable getRun() {
        return worker.getRun();
    }
}
