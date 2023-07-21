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

package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

import java.util.Collection;
import java.util.HashSet;

/**
 * An {@link AbstractExpressionParameter} that utilizes the {@link DefaultExpressionEvaluator} to generate results
 */
public class DefaultExpressionParameter extends AbstractExpressionParameter {
    private static DefaultExpressionEvaluator EVALUATOR;

    private java.util.Set<ExpressionParameterVariable> additionalUIVariables = new HashSet<>();

    public DefaultExpressionParameter() {
    }

    public DefaultExpressionParameter(String expression) {
        super(expression);
    }

    public DefaultExpressionParameter(AbstractExpressionParameter other) {
        super(other);
    }

    public static DefaultExpressionEvaluator getEvaluatorInstance() {
        if (EVALUATOR == null) {
            EVALUATOR = new DefaultExpressionEvaluator();
            // Prevent evaluator stuck without registered functions
            if (JIPipe.getInstance() != null) {
                JIPipe.getInstance().getExtensionRegisteredEventEmitter().subscribeLambda((emitter, event) -> {
                    EVALUATOR = new DefaultExpressionEvaluator();
                });
            }
        }
        return EVALUATOR;
    }

    @Override
    public ExpressionEvaluator getEvaluator() {
        return getEvaluatorInstance();
    }

    public java.util.Set<ExpressionParameterVariable> getAdditionalUIVariables() {
        return additionalUIVariables;
    }

    public void setAdditionalUIVariables(java.util.Set<ExpressionParameterVariable> additionalUIVariables) {
        this.additionalUIVariables = additionalUIVariables;
    }

    public static class List extends ListParameter<DefaultExpressionParameter> {
        public List() {
            super(DefaultExpressionParameter.class);
        }

        public List(Collection<DefaultExpressionParameter> other) {
            super(DefaultExpressionParameter.class);
            for (DefaultExpressionParameter parameter : other) {
                add(new DefaultExpressionParameter(parameter));
            }
        }
    }
}
