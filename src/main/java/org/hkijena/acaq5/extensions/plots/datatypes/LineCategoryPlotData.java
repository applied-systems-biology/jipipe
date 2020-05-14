package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

/**
 * Generates a bar category plot
 */
@ACAQDocumentation(name = "Line category plot", description = "Line chart that displays categories in its X axis and colors the lines according to the group.")
@PlotMetadata(columns = {@PlotColumn(name = "Value", description = "Values displayed in the Y axis", isNumeric = true),
        @PlotColumn(name = "Category", description = "Categories displayed in the X axis. Must correspond to each value.", isNumeric = false),
        @PlotColumn(name = "Group", description = "Groups to color the bars. Shown in the legend. Must correspond to each value.", isNumeric = false)})
public class LineCategoryPlotData extends CategoryPlotData {

    /**
     * Creates a new instance
     */
    public LineCategoryPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public LineCategoryPlotData(LineCategoryPlotData other) {
        super(other);
    }

    @Override
    public JFreeChart getChart() {
        return ChartFactory.createLineChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), createDataSet());
    }
}
