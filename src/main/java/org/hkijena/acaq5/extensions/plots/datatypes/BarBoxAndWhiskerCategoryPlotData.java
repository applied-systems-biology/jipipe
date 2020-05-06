package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

/**
 * Generates a bar category plot
 */
@ACAQDocumentation(name = "Box plot", description = "Box and whisker plot.")
@PlotMetadata(columns = {@PlotColumn(name = "Value", description = "The values", isNumeric = true),
        @PlotColumn(name = "Category", description = "Category for each value. Displayed in the X axis.", isNumeric = false),
        @PlotColumn(name = "Group", description = "Group for each value. Bars are colored by this column.", isNumeric = false)})
public class BarBoxAndWhiskerCategoryPlotData extends CategoryPlotData {
    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(getTitle(),
                getCategoryAxisLabel(),
                getValueAxisLabel(),
                (BoxAndWhiskerCategoryDataset) createDataSet(),
                true);
        CustomBoxAndWhiskerRenderer renderer = new CustomBoxAndWhiskerRenderer();
        chart.getCategoryPlot().setRenderer(renderer);
        return chart;
    }
}
