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

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

public class JIPipeSimpleIteratingAlgorithmDataBatchGenerationSettings implements JIPipeDataBatchGenerationSettings {
    private final EventBus eventBus = new EventBus();
    private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);

    public JIPipeSimpleIteratingAlgorithmDataBatchGenerationSettings() {
    }

    public JIPipeSimpleIteratingAlgorithmDataBatchGenerationSettings(JIPipeSimpleIteratingAlgorithmDataBatchGenerationSettings other) {
        this.limit = new OptionalIntegerRange(other.limit);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.")
    @JIPipeParameter(value = "limit")
    public OptionalIntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(OptionalIntegerRange limit) {
        this.limit = limit;
    }
}
