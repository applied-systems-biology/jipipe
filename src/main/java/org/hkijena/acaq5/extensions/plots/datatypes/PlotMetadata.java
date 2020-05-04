package org.hkijena.acaq5.extensions.plots.datatypes;

/**
 * Annotates a {@link PlotData} with information about plots
 */
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
