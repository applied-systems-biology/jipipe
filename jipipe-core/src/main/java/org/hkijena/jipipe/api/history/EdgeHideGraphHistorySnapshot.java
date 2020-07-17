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

import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;

public class EdgeHideGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph graph;
    private final JIPipeDataSlot source;
    private final JIPipeDataSlot target;
    private final boolean hidden;

    public EdgeHideGraphHistorySnapshot(JIPipeGraph graph, JIPipeDataSlot source, JIPipeDataSlot target, boolean hidden) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.hidden = hidden;
    }

    @Override
    public String getName() {
        return "Hide edge between " + source.getDisplayName() + " and " + target.getDisplayName();
    }

    @Override
    public void redo() {
        graph.getGraph().getEdge(source, target).setUiHidden(hidden);
    }

    @Override
    public void undo() {
        graph.getGraph().getEdge(source, target).setUiHidden(!hidden);
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

    public boolean isHidden() {
        return hidden;
    }
}
