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

package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;

import java.util.Set;

/**
 * Provides functions that allows {@link ACAQAlgorithmGraphCanvasUI} and {@link ACAQAlgorithmUI} to trigger copy & paste behavior
 */
public interface ACAQAlgorithmGraphCopyPasteBehavior {
    /**
     * Copies the set of algorithms into the clipboard
     *
     * @param algorithms set of algorithms
     */
    void copy(Set<ACAQGraphNode> algorithms);

    /**
     * Copies the set of algorithms into clipboard and removes them
     *
     * @param algorithms set of algorithms
     */
    void cut(Set<ACAQGraphNode> algorithms);

    /**
     * Pastes algorithms from clipboard
     */
    void paste();
}
