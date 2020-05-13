package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

/**
 * Generates a bar category plot
 */
@ACAQDocumentation(name = "Bar category plot", description = "Bar chart that displays categories in its X axis and colors the bars according to the group.")
@PlotMetadata(columns = {@PlotColumn(name = "Value", description = "Values displayed in the Y axis", isNumeric = true),
        @PlotColumn(name = "Category", description = "Categories displayed in the X axis. Must correspond to each value.", isNumeric = false),
        @PlotColumn(name = "Group", description = "Groups to color the bars. Shown in the legend. Must correspond to each value.", isNumeric = false)})
public class BarCategoryPlotData extends CategoryPlotData {

    /**
     * Creates a new instance
     */
    public BarCategoryPlotData() {
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public BarCategoryPlotData(BarCategoryPlotData other) {
        super(other);
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createBarChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), createDataSet());
        ((BarRenderer) chart.getCategoryPlot().getRenderer()).setBarPainter(new StandardBarPainter());
        return chart;
    }
}
