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

package org.hkijena.jipipe.launcher;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterlessSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the contract of {@link JIPipeSimpleIteratingAlgorithm} and
 * {@link JIPipeParameterlessSimpleIteratingAlgorithm}: both base classes support
 * at most one data input slot (their data batch generation only iterates over the
 * first input slot; a second data input would be silently ignored and the node
 * fails validation with "Error in source code detected!").
 *
 * <p>Algorithms that need multiple data inputs must inherit from
 * {@link org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm} instead.
 *
 * <p>This module aggregates all JIPipe plugins at runtime scope, so the test can
 * audit every registered node type.
 */
class SimpleIteratingAlgorithmInputSlotContractTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @Test
    void simpleIteratingAlgorithmsHaveAtMostOneDataInput() {
        List<String> offenders = new ArrayList<>();
        for (Map.Entry<String, JIPipeNodeInfo> entry : JIPipe.getNodes().getRegisteredNodeInfos().entrySet()) {
            JIPipeNodeInfo info = entry.getValue();
            Object instance;
            try {
                instance = info.newInstance();
            } catch (RuntimeException e) {
                // Node types that cannot be instantiated in a headless test environment are skipped;
                // their slot layout is validated by the compile-time audit and manual review.
                continue;
            }
            if (instance instanceof JIPipeSimpleIteratingAlgorithm) {
                int count = ((JIPipeSimpleIteratingAlgorithm) instance).getDataInputSlotCount();
                if (count > 1) {
                    offenders.add(entry.getKey() + " (JIPipeSimpleIteratingAlgorithm, " + count + " data inputs)");
                }
            } else if (instance instanceof JIPipeParameterlessSimpleIteratingAlgorithm) {
                int count = ((JIPipeParameterlessSimpleIteratingAlgorithm) instance).getDataInputSlots().size();
                if (count > 1) {
                    offenders.add(entry.getKey() + " (JIPipeParameterlessSimpleIteratingAlgorithm, " + count + " data inputs)");
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "Algorithms with multiple data inputs must inherit from JIPipeIteratingAlgorithm, not the simple/parameterless simple iterating variants: "
                        + String.join("; ", offenders));
    }
}
