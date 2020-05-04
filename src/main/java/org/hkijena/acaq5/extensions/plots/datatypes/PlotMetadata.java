package org.hkijena.acaq5.extensions.plots.datatypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a {@link PlotData} with information about plots
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlotMetadata {

    /**
     * @return how many data series this plot can display
     */
    int seriesCount() default 1;

    /**
     * @return This plot's columns
     */
    PlotColumn[] columns();
}
