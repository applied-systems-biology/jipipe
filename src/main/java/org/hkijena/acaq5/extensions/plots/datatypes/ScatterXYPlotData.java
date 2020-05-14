package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

/**
 * Generates a bar category plot
 */
@ACAQDocumentation(name = "XY scatter plot", description = "Plot that displays the Y values against the X values.")
@PlotMetadata(columns = {@PlotColumn(name = "X", description = "The X values", isNumeric = true),
        @PlotColumn(name = "Y", description = "The Y values", isNumeric = true)})
public class ScatterXYPlotData extends XYPlotData {

    /**
     * Creates a new instance
     */
    public ScatterXYPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ScatterXYPlotData(ScatterXYPlotData other) {
        super(other);
    }

    @Override
    public JFreeChart getChart() {
        return ChartFactory.createScatterPlot(getTitle(), getxAxisLabel(), getyAxisLabel(), createDataSet());
    }
}
