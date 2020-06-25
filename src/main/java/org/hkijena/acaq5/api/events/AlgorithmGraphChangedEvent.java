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

package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;

/**
 * Event is triggered when algorithm graph is changed
 */
public class AlgorithmGraphChangedEvent {
    private ACAQAlgorithmGraph algorithmGraph;

    /**
     * @param algorithmGraph the graph
     */
    public AlgorithmGraphChangedEvent(ACAQAlgorithmGraph algorithmGraph) {
        this.algorithmGraph = algorithmGraph;
    }

    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }
}
