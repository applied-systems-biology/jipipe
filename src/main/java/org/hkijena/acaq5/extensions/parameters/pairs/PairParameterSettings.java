package org.hkijena.acaq5.extensions.parameters.pairs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to control the behavior of {@link Pair}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PairParameterSettings {
    /**
     * @return Optional label for the key
     */
    String keyLabel() default "";

    /**
     * @return Optional label for the value
     */
    String valueLabel() default "";

    /**
     * @return If the parameters are shown in one row - separated with an arrow. Otherwise display them in two rows
     */
    boolean singleRow() default true;

    /**
     * @return If a chevron/arrow is shown in the single row mode
     */
    boolean singleRowWithChevron() default true;
}
