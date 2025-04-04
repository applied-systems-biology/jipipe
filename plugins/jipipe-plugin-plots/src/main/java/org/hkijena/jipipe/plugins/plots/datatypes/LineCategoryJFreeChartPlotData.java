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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;

import java.awt.*;

/**
 * Generates a bar category plot
 * Series table columns: Value (Double), Category (String), Group (String)
 * Multiple series: No
 */
@SetJIPipeDocumentation(name = "JFreeChart Line category plot", description = "Line chart that displays categories in its X axis and colors the lines according to the group.")
@JFreeChartPlotMetadata(columns = {@JFreeChartPlotColumn(name = "Value", description = "Values displayed in the Y axis", isNumeric = true),
        @JFreeChartPlotColumn(name = "Category", description = "Categories displayed in the X axis. Must correspond to each value.", isNumeric = false),
        @JFreeChartPlotColumn(name = "Group", description = "Groups to color the bars. Shown in the legend. Must correspond to each value.", isNumeric = false)}, maxSeriesCount = 1)
public class LineCategoryJFreeChartPlotData extends CategoryJFreeChartPlotData {

    private int lineThickness = 1;

    /**
     * Creates a new instance
     */
    public LineCategoryJFreeChartPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public LineCategoryJFreeChartPlotData(LineCategoryJFreeChartPlotData other) {
        super(other);
        this.lineThickness = other.lineThickness;
    }

    public static LineCategoryJFreeChartPlotData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return JFreeChartPlotData.importData(storage, LineCategoryJFreeChartPlotData.class, progressInfo);
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createLineChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), createDataSet());
        chart.getCategoryPlot().setDomainGridlinePaint(getGridColor());
        chart.getCategoryPlot().getDomainAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getCategoryAxisFontSize()));
        chart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getCategoryAxisFontSize()));
        chart.getCategoryPlot().getRangeAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getValueAxisFontSize()));
        chart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getValueAxisFontSize()));

        calibrateAxis(chart.getCategoryPlot().getRangeAxis(), getValueAxisMinimum(), getValueAxisMaximum());

        // Set line thickness
        CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer();
        renderer.setDefaultStroke(new BasicStroke(lineThickness));
        ((AbstractRenderer) renderer).setAutoPopulateSeriesStroke(false);

        // Default update
        updateChartProperties(chart);
        return chart;
    }

    @SetJIPipeDocumentation(name = "Line thickness", description = "The thickness of the lines")
    @JIPipeParameter("line-thickness")
    public int getLineThickness() {
        return lineThickness;
    }

    @JIPipeParameter("line-thickness")
    public void setLineThickness(int lineThickness) {
        this.lineThickness = lineThickness;
    }
}
