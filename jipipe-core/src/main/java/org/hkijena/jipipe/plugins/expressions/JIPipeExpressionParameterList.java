package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

import java.util.Collection;

public class JIPipeExpressionParameterList extends JIPipeListParameter<JIPipeExpressionParameter> {
    public JIPipeExpressionParameterList() {
        super(JIPipeExpressionParameter.class);
    }

    public JIPipeExpressionParameterList(Collection<JIPipeExpressionParameter> other) {
        super(JIPipeExpressionParameter.class);
        for (JIPipeExpressionParameter parameter : other) {
            add(new JIPipeExpressionParameter(parameter));
        }
    }
}
