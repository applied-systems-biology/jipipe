package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.extensions.parameters.optional.OptionalParameter;

public class OptionalDefaultExpressionParameter extends OptionalParameter<DefaultExpressionParameter> {

    public OptionalDefaultExpressionParameter() {
        super(DefaultExpressionParameter.class);
        setContent(new DefaultExpressionParameter());
    }

    public OptionalDefaultExpressionParameter(boolean enabled, String expression) {
        super(DefaultExpressionParameter.class);
        setEnabled(enabled);
        setContent(new DefaultExpressionParameter(expression));
    }

    public OptionalDefaultExpressionParameter(OptionalParameter<DefaultExpressionParameter> other) {
        super(other);
        this.setContent(new DefaultExpressionParameter(other.getContent()));
    }
}
