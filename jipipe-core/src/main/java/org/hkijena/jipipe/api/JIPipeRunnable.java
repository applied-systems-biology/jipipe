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
}