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

package org.hkijena.jipipe.extensions.plots.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

import java.awt.*;
import java.nio.file.Path;

/**
 * A stacked bar plot
 * Series table columns: Value (Double), Category (String), Group (String)
 * Multiple series: No
 */
@JIPipeDocumentation(name = "Stacked bar category plot", description = "Bar chart that displays categories in its X axis and colors the bars according to the group." +
        " Bars within the same group are stacked.")
@PlotMetadata(columns = {@PlotColumn(name = "Value", description = "Values displayed in the Y axis", isNumeric = true),
        @PlotColumn(name = "Category", description = "Categories displayed in the X axis. Must correspond to each value.", isNumeric = false),
        @PlotColumn(name = "Group", description = "Groups to color the bars. Shown in the legend. Must correspond to each value.", isNumeric = false)}, maxSeriesCount = 1)
public class StackedBarCategoryPlotData extends CategoryPlotData {

    /**
     * Creates a new instance
     */
    public StackedBarCategoryPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StackedBarCategoryPlotData(StackedBarCategoryPlotData other) {
        super(other);
    }

    public static StackedBarCategoryPlotData importFrom(Path storagePath) {
        return PlotData.importFrom(storagePath, StackedBarCategoryPlotData.class);
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createStackedBarChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), createDataSet());
        ((BarRenderer) chart.getCategoryPlot().getRenderer()).setBarPainter(new StandardBarPainter());
        chart.getCategoryPlot().setDomainGridlinePaint(getGridColor());
        chart.getCategoryPlot().getDomainAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getCategoryAxisFontSize()));
        chart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getCategoryAxisFontSize()));
        chart.getCategoryPlot().getRangeAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getValueAxisFontSize()));
        chart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getValueAxisFontSize()));

        calibrateAxis(chart.getCategoryPlot().getRangeAxis(), getValueAxisMinimum(), getValueAxisMaximum());

        updateChartProperties(chart);
        return chart;
    }
}
