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

import java.util.List;
import java.util.stream.Collectors;

public class CompoundGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final List<JIPipeAlgorithmGraphHistorySnapshot> operations;

    public CompoundGraphHistorySnapshot(List<JIPipeAlgorithmGraphHistorySnapshot> operations) {
        this.operations = operations;
    }

    @Override
    public String getName() {
        return operations.stream().map(JIPipeAlgorithmGraphHistorySnapshot::getName).collect(Collectors.joining(", "));
    }

    @Override
    public void undo() {
        for (int i = operations.size() - 1; i >= 0; --i) {
            operations.get(i).undo();
        }
    }

    @Override
    public void redo() {
        for (JIPipeAlgorithmGraphHistorySnapshot operation : operations) {
            operation.redo();
        }
    }

    public List<JIPipeAlgorithmGraphHistorySnapshot> getOperations() {
        return operations;
    }
}
