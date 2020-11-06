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

package org.hkijena.jipipe.installer.linux.api;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runnable that can be scheduled, canceled, and reports progress
 */
public interface JIPipeRunnable {
    /**
     * Runs the runnable
     *
     * @param onProgress  Function that consumes progress reports
     * @param isCancelled Function that supplies if the runnable should be canceled
     */
    void run(Consumer<JIPipeRunnerStatus> onProgress, Supplier<Boolean> isCancelled);
}
