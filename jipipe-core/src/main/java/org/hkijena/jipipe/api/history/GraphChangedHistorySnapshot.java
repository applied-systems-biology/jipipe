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

import org.hkijena.jipipe.api.nodes.JIPipeGraph;

public class GraphChangedHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph targetGraph;
    private final JIPipeGraph backupGraph;
    private final String name;
    private JIPipeGraph changedGraph;

    public GraphChangedHistorySnapshot(JIPipeGraph targetGraph, String name) {
        this.targetGraph = targetGraph;
        this.backupGraph = new JIPipeGraph(targetGraph);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void undo() {
        changedGraph = new JIPipeGraph(targetGraph);
        targetGraph.replaceWith(new JIPipeGraph(backupGraph));
    }

    @Override
    public void redo() {
        if (changedGraph != null) {
            targetGraph.replaceWith(new JIPipeGraph(changedGraph));
        }
    }

    public JIPipeGraph getTargetGraph() {
        return targetGraph;
    }

    public JIPipeGraph getBackupGraph() {
        return backupGraph;
    }

    public JIPipeGraph getChangedGraph() {
        return changedGraph;
    }

    public void setChangedGraph(JIPipeGraph changedGraph) {
        this.changedGraph = new JIPipeGraph(changedGraph);
    }
}
