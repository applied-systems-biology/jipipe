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

package org.hkijena.acaq5.api.history;

import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class EdgeDisconnectGraphHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQGraph graph;
    private final ACAQDataSlot source;
    private final ACAQDataSlot target;

    public EdgeDisconnectGraphHistorySnapshot(ACAQGraph graph, ACAQDataSlot source, ACAQDataSlot target) {
        this.graph = graph;
        this.source = source;
        this.target = target;
    }

    @Override
    public String getName() {
        return "Disconnect " + source.getNameWithAlgorithmName() + " and " + target.getNameWithAlgorithmName();
    }

    @Override
    public void undo() {
        graph.connect(source, target, true);
    }

    @Override
    public void redo() {
        graph.disconnect(source, target, false);
    }

    public ACAQGraph getGraph() {
        return graph;
    }

    public ACAQDataSlot getSource() {
        return source;
    }

    public ACAQDataSlot getTarget() {
        return target;
    }
}
