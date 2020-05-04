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
     * @return the data type of this column. Should be double or String
     */
    Class<?> dataType();
}
