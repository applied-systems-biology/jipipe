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
 * <p>
 * The enum NAMES are not the wire contract: the NDJSON wire literals are produced by the explicit
 * mappings in {@link AbstractJIPipeProgressLog} — {@code mapNodeStateToWire} writes
 * {@code ok|fail|cancel} into the {@code s} field of {@code N} records, and
 * {@code mapRunStateToWire} writes {@code running|finished|failed|cancelled} into the {@code s}
 * field of {@code s} records. Only those literals and the mapping methods are part of the
 * NDJSON contract (a renamed or added constant changes nothing on disk unless the mappings change).
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
