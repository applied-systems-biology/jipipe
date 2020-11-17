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
import java.util.stream.Collectors;

public class EdgeDisconnectGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph graph;
    private final Set<JIPipeDataSlot> sources;
    private final JIPipeDataSlot target;

    public EdgeDisconnectGraphHistorySnapshot(JIPipeGraph graph, Set<JIPipeDataSlot> sources, JIPipeDataSlot target) {
        this.graph = graph;
        this.sources = sources;
        this.target = target;
    }

    @Override
    public String getName() {

        return "Disconnect " + sources.stream().map(JIPipeDataSlot::getDisplayName).collect(Collectors.joining(", ")) + " and " + target.getDisplayName();
    }

    @Override
    public void undo() {
        for (JIPipeDataSlot source : sources) {
            graph.connect(source, target, true);
        }
    }

    @Override
    public void redo() {
        for (JIPipeDataSlot source : sources) {
            graph.disconnect(source, target, false);
        }
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public Set<JIPipeDataSlot> getSources() {
        return sources;
    }

    public JIPipeDataSlot getTarget() {
        return target;
    }
}
