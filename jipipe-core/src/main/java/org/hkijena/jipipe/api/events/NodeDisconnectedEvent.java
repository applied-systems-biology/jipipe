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

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;

/**
 * Generated when slots are disconnected
 */
public class NodeDisconnectedEvent {
    private JIPipeGraph graph;
    private JIPipeDataSlot source;
    private JIPipeDataSlot target;

    /**
     * @param graph  the graph
     * @param source the source slot
     * @param target the target slot
     */
    public NodeDisconnectedEvent(JIPipeGraph graph, JIPipeDataSlot source, JIPipeDataSlot target) {
        this.graph = graph;
        this.source = source;
        this.target = target;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public JIPipeDataSlot getSource() {
        return source;
    }

    public JIPipeDataSlot getTarget() {
        return target;
    }
}
