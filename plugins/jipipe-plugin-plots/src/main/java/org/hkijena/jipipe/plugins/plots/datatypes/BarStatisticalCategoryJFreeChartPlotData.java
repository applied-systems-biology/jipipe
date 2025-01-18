/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.plots.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;

import java.awt.*;

/**
 * A statistical bar category plot
 * Series table columns: Value (Double), Category (String), Group (String)
 * Multiple series: No
 */
@SetJIPipeDocumentation(name = "JFreeChart Statistical bar category plot", description = "Bar chart that displays categories in its X axis and colors the bars according to the group. " +
        "The bar height is the mean of each condition's values. Shows an error bar.")
@JFreeChartPlotMetadata(columns = {@JFreeChartPlotColumn(name = "Value", description = "Values displayed in the Y axis", isNumeric = true),
        @JFreeChartPlotColumn(name = "Category", description = "Categories displayed in the X axis. Must correspond to each value.", isNumeric = false),
        @JFreeChartPlotColumn(name = "Group", description = "Groups to color the bars. Shown in the legend. Must correspond to each value.", isNumeric = false)}, maxSeriesCount = 1)
public class BarStatisticalCategoryJFreeChartPlotData extends StatisticalCategoryJFreeChartPlotData {

    /**
     * Creates a new instance
     */
    public BarStatisticalCategoryJFreeChartPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public BarStatisticalCategoryJFreeChartPlotData(BarStatisticalCategoryJFreeChartPlotData other) {
        super(other);
    }

    public static BarStatisticalCategoryJFreeChartPlotData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return JFreeChartPlotData.importData(storage, BarStatisticalCategoryJFreeChartPlotData.class, progressInfo);
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createBarChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), createDataSet());
        chart.getCategoryPlot().setRenderer(new StatisticalBarRenderer());
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
