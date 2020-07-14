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

package org.hkijena.jipipe.api.events;

import org.hkijena.jipipe.api.algorithm.JIPipeGraph;

/**
 * Event is triggered when algorithm graph is changed
 */
public class GraphChangedEvent {
    private JIPipeGraph algorithmGraph;

    /**
     * @param algorithmGraph the graph
     */
    public GraphChangedEvent(JIPipeGraph algorithmGraph) {
        this.algorithmGraph = algorithmGraph;
    }

    public JIPipeGraph getAlgorithmGraph() {
        return algorithmGraph;
    }
}
