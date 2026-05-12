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

package org.hkijena.jipipe.api.nodes;

/**
 * Classification of a node that affects search ranking.
 * Standard nodes are shown at full rank, AutoImport nodes are downranked,
 * and EdgeCase nodes are further downranked.
 */
public enum JIPipeNodeClassification {
    /**
     * Standard node - full search rank
     */
    Standard,

    /**
     * Auto-imported node (e.g., from IJ2 or CLIJ) - downranked in search
     */
    AutoImport,

    /**
     * Edge case node - further downranked in search
     */
    EdgeCase
}
