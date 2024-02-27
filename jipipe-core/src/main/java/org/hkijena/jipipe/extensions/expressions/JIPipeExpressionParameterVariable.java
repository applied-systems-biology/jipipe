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

package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.extensions.expressions.variables.UndefinedExpressionParameterVariablesInfo;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Alternative to creating an {@link ExpressionParameterVariablesInfo} class and attaching it to {@link JIPipeExpressionParameterSettings}.
 * If an {@link ExpressionParameterVariablesInfo} is provided, this variable is merged into the list of variables.
 * You can leave name(), description() and key() empty if you provide fromClass()
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(JIPipeExpressionParameterVariables.class)
public @interface JIPipeExpressionParameterVariable {
    String name() default "";

    String description() default "";

    String key() default "";

    Class<? extends ExpressionParameterVariablesInfo> fromClass() default UndefinedExpressionParameterVariablesInfo.class;
}
