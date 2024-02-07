package org.hkijena.jipipe.extensions.expressions;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Repeats {@link JIPipeExpressionParameterVariable}
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JIPipeExpressionParameterVariables {
    JIPipeExpressionParameterVariable[] value();
}
