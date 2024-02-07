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
