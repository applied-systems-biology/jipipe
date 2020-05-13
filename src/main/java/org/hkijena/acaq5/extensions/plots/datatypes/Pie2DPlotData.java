package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

/**
 * Generates a bar category plot
 */
@ACAQDocumentation(name = "2D pie plot", description = "Plot that shows the amount for each category as slice in a pie.")
@PlotMetadata(columns = {@PlotColumn(name = "Amount", description = "The values to be displayed", isNumeric = true),
        @PlotColumn(name = "Category", description = "The categories to be displayed", isNumeric = false)})
public class Pie2DPlotData extends PiePlotData {

    /**
     * Creates a new instance
     */
    public Pie2DPlotData() {
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public Pie2DPlotData(Pie2DPlotData other) {
        super(other);
    }

    @Override
    public JFreeChart getChart() {
        return ChartFactory.createPieChart(getTitle(), createDataSet(), true, true, false);
    }
}
