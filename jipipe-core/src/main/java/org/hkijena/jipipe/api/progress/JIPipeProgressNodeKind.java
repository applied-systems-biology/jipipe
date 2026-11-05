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
 * Kind of a node inside the progress tree.
 * The enum name is part of the NDJSON wire contract ({@code k} field of {@code n} records) and must not be renamed.
 */
public enum JIPipeProgressNodeKind {

    /**
     * A partition node that groups its descendants (rendered as a dim chip in the UI).
     */
    PARTITION,

    /**
     * A phase node (for example preprocessing or a stage inside an algorithm).
     */
    PHASE,

    /**
     * A node that corresponds to one processed item or algorithm.
     */
    ITEM,

    /**
     * The root node of a run.
     */
    RUN
}
