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

import org.hkijena.jipipe.extensions.expressions.variables.UndefinedExpressionParameterVariableSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controls the behavior of an {@link ExpressionParameter}.
 * Attach it to the getter or setter within a {@link org.hkijena.jipipe.api.parameters.JIPipeParameterCollection}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpressionParameterSettings {
    /**
     * Allows to give hints to the editor UI which variables are available.
     * The variable source instance has access to the {@link org.hkijena.jipipe.api.parameters.JIPipeParameterAccess}.
     *
     * @return the variable source class
     */
    Class<? extends ExpressionParameterVariableSource> variableSource() default UndefinedExpressionParameterVariableSource.class;
}
