package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;

/**
 * Generates a bar category plot
 */
@ACAQDocumentation(name = "Statistical line category plot", description = "Line chart that displays categories in its X axis and colors the lines according to the group. " +
        "The line Y value is the mean of each condition's values. Shows an error bar.")
@PlotMetadata(columns = {@PlotColumn(name = "Value", description = "Values displayed in the Y axis", isNumeric = true),
        @PlotColumn(name = "Category", description = "Categories displayed in the X axis. Must correspond to each value.", isNumeric = false),
        @PlotColumn(name = "Group", description = "Groups to color the bars. Shown in the legend. Must correspond to each value.", isNumeric = false)})
public class LineStatisticalCategoryPlotData extends CategoryPlotData {

    /**
     * Creates a new instance
     */
    public LineStatisticalCategoryPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public LineStatisticalCategoryPlotData(LineStatisticalCategoryPlotData other) {
        super(other);
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createLineChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), createDataSet());
        chart.getCategoryPlot().setRenderer(new StatisticalLineAndShapeRenderer());
        return chart;
    }
}
