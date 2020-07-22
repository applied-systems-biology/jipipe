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

package org.hkijena.jipipe.api.history;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;

import java.util.Set;

public class EdgeDisconnectAllTargetsGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph graph;
    private final JIPipeDataSlot source;
    private final Set<JIPipeDataSlot> targets;

    public EdgeDisconnectAllTargetsGraphHistorySnapshot(JIPipeGraph graph, JIPipeDataSlot source) {
        this.graph = graph;
        this.source = source;
        this.targets = graph.getTargetSlots(source);
    }

    @Override
    public String getName() {
        return "Disconnect all targets of " + source.getDisplayName();
    }

    @Override
    public void undo() {
        for (JIPipeDataSlot target : targets) {
            graph.connect(source, target, true);
        }
    }

    @Override
    public void redo() {
        graph.disconnectAll(source, false);
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public JIPipeDataSlot getSource() {
        return source;
    }
}
