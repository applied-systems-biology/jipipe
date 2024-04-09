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

package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;

public class OptionalJIPipeExpressionParameter extends OptionalParameter<JIPipeExpressionParameter> {

    public OptionalJIPipeExpressionParameter() {
        super(JIPipeExpressionParameter.class);
        setContent(new JIPipeExpressionParameter());
    }

    public OptionalJIPipeExpressionParameter(boolean enabled, String expression) {
        super(JIPipeExpressionParameter.class);
        setEnabled(enabled);
        setContent(new JIPipeExpressionParameter(expression));
    }

    public OptionalJIPipeExpressionParameter(OptionalParameter<JIPipeExpressionParameter> other) {
        super(other);
        this.setContent(new JIPipeExpressionParameter(other.getContent()));
    }
}
