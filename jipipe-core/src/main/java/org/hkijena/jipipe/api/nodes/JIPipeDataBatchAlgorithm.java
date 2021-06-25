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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.util.List;

/**
 * Methods shared across all {@link JIPipeAlgorithm} that generate data batches
 */
public interface JIPipeDataBatchAlgorithm {
    /**
     * Returns the batch generation settings as interface
     *
     * @return batch generation settings as interface
     */
    JIPipeParameterCollection getGenerationSettingsInterface();

    /**
     * Generates data batches.
     * This is a dry-run function that should never throw errors
     *
     * @param slots the data slots
     * @param progressInfo the progress
     * @return the batches
     */
    List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeDataSlot> slots, JIPipeProgressInfo progressInfo);
}
