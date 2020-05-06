package org.hkijena.acaq5.extensions.plots.datatypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines that a {@link PlotData} has
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlotColumn {
    /**
     * @return the identifier of this column
     */
    String name();

    /**
     * @return a short description
     */
    String description();

    /**
     * @return if the column is numeric. In this case, it is converted to double. Otherwise it is assumed the row is {@link String}
     */
    boolean isNumeric();
}
