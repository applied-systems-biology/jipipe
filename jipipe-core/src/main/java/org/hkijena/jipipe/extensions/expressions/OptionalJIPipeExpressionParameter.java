package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.extensions.parameters.api.optional.OptionalParameter;

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
