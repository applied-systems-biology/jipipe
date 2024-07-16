/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ijfilaments.parameters;

import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.jgrapht.alg.cycle.PatonCycleBase;
import org.jgrapht.alg.cycle.QueueBFSFundamentalCycleBasis;
import org.jgrapht.alg.cycle.StackBFSFundamentalCycleBasis;
import org.jgrapht.alg.interfaces.CycleBasisAlgorithm;

import java.util.List;
import java.util.Set;

public enum CycleFinderAlgorithm {
    PatonCycleBasis,
    StackBFSCycleBasis,
    QueueBFSCycleBasis;

    public Set<List<FilamentEdge>> findCycles(Filaments3DData graph) {
        switch (this) {
            case PatonCycleBasis: {
                PatonCycleBase<FilamentVertex, FilamentEdge> patonCycleBase = new PatonCycleBase<>(graph);
                CycleBasisAlgorithm.CycleBasis<FilamentVertex, FilamentEdge> cycleBasis = patonCycleBase.getCycleBasis();
                return cycleBasis.getCycles();
            }
            case StackBFSCycleBasis: {
                StackBFSFundamentalCycleBasis<FilamentVertex, FilamentEdge> stackBFS = new StackBFSFundamentalCycleBasis<>(graph);
                CycleBasisAlgorithm.CycleBasis<FilamentVertex, FilamentEdge> cycleBasis = stackBFS.getCycleBasis();
                return cycleBasis.getCycles();
            }
            case QueueBFSCycleBasis: {
                QueueBFSFundamentalCycleBasis<FilamentVertex, FilamentEdge> stackBFS = new QueueBFSFundamentalCycleBasis<>(graph);
                CycleBasisAlgorithm.CycleBasis<FilamentVertex, FilamentEdge> cycleBasis = stackBFS.getCycleBasis();
                return cycleBasis.getCycles();
            }
            default:
                throw new UnsupportedOperationException();
        }
    }
}
