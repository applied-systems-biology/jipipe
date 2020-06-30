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

public class GraphChangedHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQGraph targetGraph;
    private final ACAQGraph backupGraph;
    private ACAQGraph changedGraph;
    private final String name;

    public GraphChangedHistorySnapshot(ACAQGraph targetGraph, String name) {
        this.targetGraph = targetGraph;
        this.backupGraph = new ACAQGraph(targetGraph);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void undo() {
        changedGraph = new ACAQGraph(targetGraph);
        targetGraph.replaceWith(new ACAQGraph(backupGraph));
    }

    @Override
    public void redo() {
        if(changedGraph != null) {
            targetGraph.replaceWith(new ACAQGraph(changedGraph));
        }
    }

    public ACAQGraph getTargetGraph() {
        return targetGraph;
    }

    public ACAQGraph getBackupGraph() {
        return backupGraph;
    }

    public ACAQGraph getChangedGraph() {
        return changedGraph;
    }

    public void setChangedGraph(ACAQGraph changedGraph) {
        this.changedGraph = new ACAQGraph(changedGraph);
    }
}
