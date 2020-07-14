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

package org.hkijena.jipipe.api.algorithm;

import gnu.trove.set.TIntSet;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.util.Map;

/**
 * Methods shared across all {@link JIPipeAlgorithm} that generate data batches
 */
public interface JIPipeDataBatchAlgorithm {
    /**
     * Returns the batch generation settings as interface
     * @return batch generation settings as interface
     */
    JIPipeParameterCollection getGenerationSettingsInterface();

    /**
     * Generate the initial data batches based on the slot map
     * @param slotMap the slot map
     * @return data batches
     */
    Map<JIPipeDataBatchKey, Map<String, TIntSet>> generateBatches(Map<String, JIPipeDataSlot> slotMap);
}
