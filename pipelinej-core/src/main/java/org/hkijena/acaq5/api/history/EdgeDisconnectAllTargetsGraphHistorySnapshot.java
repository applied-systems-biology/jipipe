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

import java.util.Set;

public class EdgeDisconnectAllTargetsGraphHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQGraph graph;
    private final ACAQDataSlot source;
    private final Set<ACAQDataSlot> targets;

    public EdgeDisconnectAllTargetsGraphHistorySnapshot(ACAQGraph graph, ACAQDataSlot source) {
        this.graph = graph;
        this.source = source;
        this.targets = graph.getTargetSlots(source);
    }

    @Override
    public String getName() {
        return "Disconnect all targets of " + source.getNameWithAlgorithmName();
    }

    @Override
    public void undo() {
        for (ACAQDataSlot target : targets) {
            graph.connect(source, target, true);
        }
    }

    @Override
    public void redo() {
        graph.disconnectAll(source, false);
    }

    public ACAQGraph getGraph() {
        return graph;
    }

    public ACAQDataSlot getSource() {
        return source;
    }
}
