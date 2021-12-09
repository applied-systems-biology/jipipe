package org.hkijena.jipipe.extensions.parameters.roi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to modify the behavior of the {@link Anchor} parameter editor UI
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AnchorParameterSettings {
    boolean allowTopLeft() default true;

    boolean allowTopCenter() default true;

    boolean allowTopRight() default true;

    boolean allowBottomLeft() default true;

    boolean allowBottomCenter() default true;

    boolean allowBottomRight() default true;

    boolean allowCenterLeft() default true;

    boolean allowCenterRight() default true;

    boolean allowCenterCenter() default true;
}
