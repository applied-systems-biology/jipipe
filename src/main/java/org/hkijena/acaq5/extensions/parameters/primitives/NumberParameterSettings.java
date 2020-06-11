package org.hkijena.acaq5.extensions.parameters.primitives;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attached to the getter or setter of {@link Number} parameter to setup the GUI
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NumberParameterSettings {
    /**
     * Minimum value
     *
     * @return Minimum value
     */
    double min() default Double.NEGATIVE_INFINITY;

    /**
     * Maximum value
     *
     * @return Maximum value
     */
    double max() default Double.POSITIVE_INFINITY;

    /**
     * The step size when the user clicks increase/decrease
     *
     * @return step size when the user clicks increase/decrease
     */
    double step() default 1;
}
