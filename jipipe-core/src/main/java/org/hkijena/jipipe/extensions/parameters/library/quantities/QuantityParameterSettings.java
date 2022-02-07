package org.hkijena.jipipe.extensions.parameters.library.quantities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface QuantityParameterSettings {
    /**
     * List of predefined units that are displayed to the user
     *
     * @return units
     */
    String[] predefinedUnits() default {};
}
