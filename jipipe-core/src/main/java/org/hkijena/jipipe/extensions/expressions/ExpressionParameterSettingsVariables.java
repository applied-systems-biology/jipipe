package org.hkijena.jipipe.extensions.expressions;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Repeats {@link ExpressionParameterSettingsVariable}
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExpressionParameterSettingsVariables {
    ExpressionParameterSettingsVariable[] value();
}
