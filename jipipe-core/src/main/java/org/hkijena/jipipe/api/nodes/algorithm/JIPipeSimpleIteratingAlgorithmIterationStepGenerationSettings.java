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

package org.hkijena.jipipe.api.nodes.algorithm;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;

public class JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings extends AbstractJIPipeParameterCollection implements JIPipeIterationStepGenerationSettings {
    private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);

    public JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings() {
    }

    public JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings(JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings other) {
        this.limit = new OptionalIntegerRange(other.limit);
    }

    @SetJIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.")
    @JIPipeParameter(value = "limit")
    public OptionalIntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(OptionalIntegerRange limit) {
        this.limit = limit;
    }
}
