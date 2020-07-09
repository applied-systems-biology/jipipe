/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.extensions.plots.datatypes;

import org.hkijena.pipelinej.api.ACAQDocumentation;
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
