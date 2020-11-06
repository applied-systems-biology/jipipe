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

package org.hkijena.jipipe.installer.linux.ui.events;

import org.hkijena.jipipe.installer.linux.api.JIPipeRunnable;
import org.hkijena.jipipe.installer.linux.api.JIPipeRunnerStatus;
import org.hkijena.jipipe.installer.linux.ui.utils.JIPipeRunWorker;

/**
 * Generated when an {@link JIPipeRunWorker} reports progress
 */
public class RunUIWorkerProgressEvent {
    private JIPipeRunWorker worker;
    private JIPipeRunnerStatus status;

    /**
     * @param worker the worker
     * @param status the progress status
     */
    public RunUIWorkerProgressEvent(JIPipeRunWorker worker, JIPipeRunnerStatus status) {
        this.worker = worker;
        this.status = status;
    }

    public JIPipeRunWorker getWorker() {
        return worker;
    }

    public JIPipeRunnable getRun() {
        return worker.getRun();
    }

    public JIPipeRunnerStatus getStatus() {
        return status;
    }
}
