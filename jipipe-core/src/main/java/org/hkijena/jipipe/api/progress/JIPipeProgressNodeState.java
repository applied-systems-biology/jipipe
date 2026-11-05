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

package org.hkijena.jipipe.api.progress;

/**
 * State of a node inside the progress tree.
 * The enum name is part of the NDJSON wire contract ({@code s} field of {@code N} and {@code s} records) and must not be renamed.
 */
public enum JIPipeProgressNodeState {

    /**
     * The node or run is still executing.
     */
    RUNNING,

    /**
     * The node or run finished successfully.
     */
    OK,

    /**
     * The node or run failed.
     */
    FAILED,

    /**
     * The node or run was cancelled.
     */
    CANCELLED
}
