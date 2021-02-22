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

package org.hkijena.jipipe.extensions.parameters.expressions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;

/**
 * An {@link ExpressionParameter} that utilizes the {@link DefaultExpressionEvaluator} to generate results
 */
public class DefaultExpressionParameter extends ExpressionParameter {
    private static DefaultExpressionEvaluator EVALUATOR;

    public DefaultExpressionParameter() {
    }

    public DefaultExpressionParameter(String expression) {
        super(expression);
    }

    public DefaultExpressionParameter(ExpressionParameter other) {
        super(other);
    }

    public static DefaultExpressionEvaluator getEvaluatorInstance() {
        if (EVALUATOR == null) {
            EVALUATOR = new DefaultExpressionEvaluator();
            // Prevent evaluator stuck without registered functions
            if (JIPipe.getInstance() != null) {
                JIPipe.getInstance().getEventBus().register(new Object() {
                    @Subscribe
                    public void onExtensionRegistered(JIPipe.ExtensionRegisteredEvent event) {
                        EVALUATOR = new DefaultExpressionEvaluator();
                    }
                });
            }
        }
        return EVALUATOR;
    }

    @Override
    public ExpressionEvaluator getEvaluator() {
        return getEvaluatorInstance();
    }
}
