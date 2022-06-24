package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.extensions.expressions.variables.UndefinedExpressionParameterVariableSource;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Alternative to creating an {@link ExpressionParameterVariableSource} class and attaching it to {@link ExpressionParameterSettings}.
 * If an {@link ExpressionParameterVariableSource} is provided, this variable is merged into the list of variables.
 * You can leave name(), description() and key() empty if you provide fromClass()
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(ExpressionParameterSettingsVariables.class)
public @interface ExpressionParameterSettingsVariable {
    String name() default "";

    String description() default "";

    String key() default "";

    Class<? extends ExpressionParameterVariableSource> fromClass() default UndefinedExpressionParameterVariableSource.class;
}
