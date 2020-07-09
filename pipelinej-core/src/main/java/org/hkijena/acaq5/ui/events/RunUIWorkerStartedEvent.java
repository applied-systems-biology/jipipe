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
 * Generated when an {@link ACAQRunWorker} was started
 */
public class RunUIWorkerStartedEvent {

    private ACAQRunnable run;
    private ACAQRunWorker worker;

    /**
     * @param run    the run
     * @param worker the worker
     */
    public RunUIWorkerStartedEvent(ACAQRunnable run, ACAQRunWorker worker) {
        this.run = run;
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public ACAQRunnable getRun() {
        return run;
    }
}
