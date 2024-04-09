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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;

import java.util.Collection;
import java.util.HashSet;

/**
 * An {@link AbstractExpressionParameter} that utilizes the {@link JIPipeExpressionEvaluator} to generate results
 */
public class JIPipeExpressionParameter extends AbstractExpressionParameter {
    private static JIPipeExpressionEvaluator EVALUATOR;

    private java.util.Set<JIPipeExpressionParameterVariableInfo> additionalUIVariables = new HashSet<>();

    public JIPipeExpressionParameter() {
    }

    public JIPipeExpressionParameter(String expression) {
        super(expression);
    }

    public JIPipeExpressionParameter(AbstractExpressionParameter other) {
        super(other);
    }

    public static JIPipeExpressionEvaluator getEvaluatorInstance() {
        if (EVALUATOR == null) {
            EVALUATOR = new JIPipeExpressionEvaluator();
            // Prevent evaluator stuck without registered functions
            if (JIPipe.getInstance() != null) {
                JIPipe.getInstance().getExtensionRegisteredEventEmitter().subscribeLambda((emitter, event) -> {
                    EVALUATOR = new JIPipeExpressionEvaluator();
                });
            }
        }
        return EVALUATOR;
    }

    @Override
    public ExpressionEvaluator getEvaluator() {
        return getEvaluatorInstance();
    }

    public java.util.Set<JIPipeExpressionParameterVariableInfo> getAdditionalUIVariables() {
        return additionalUIVariables;
    }

    public void setAdditionalUIVariables(java.util.Set<JIPipeExpressionParameterVariableInfo> additionalUIVariables) {
        this.additionalUIVariables = additionalUIVariables;
    }

    public static class List extends ListParameter<JIPipeExpressionParameter> {
        public List() {
            super(JIPipeExpressionParameter.class);
        }

        public List(Collection<JIPipeExpressionParameter> other) {
            super(JIPipeExpressionParameter.class);
            for (JIPipeExpressionParameter parameter : other) {
                add(new JIPipeExpressionParameter(parameter));
            }
        }
    }
}
