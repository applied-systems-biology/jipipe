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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import java.awt.BasicStroke;
import java.awt.Font;
import java.nio.file.Path;

/**
 * Generates a bar category plot
 */
@JIPipeDocumentation(name = "Line category plot", description = "Line chart that displays categories in its X axis and colors the lines according to the group.")
@PlotMetadata(columns = {@PlotColumn(name = "Value", description = "Values displayed in the Y axis", isNumeric = true),
        @PlotColumn(name = "Category", description = "Categories displayed in the X axis. Must correspond to each value.", isNumeric = false),
        @PlotColumn(name = "Group", description = "Groups to color the bars. Shown in the legend. Must correspond to each value.", isNumeric = false)})
public class LineCategoryPlotData extends CategoryPlotData {

    private int lineThickness = 1;

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
        this.lineThickness = other.lineThickness;
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createLineChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), createDataSet());
        chart.getCategoryPlot().setDomainGridlinePaint(getGridColor());
        chart.getCategoryPlot().getDomainAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getCategoryAxisFontSize()));
        chart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getCategoryAxisFontSize()));
        chart.getCategoryPlot().getRangeAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getValueAxisFontSize()));
        chart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getValueAxisFontSize()));

        // Set line thickness
        CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer();
        renderer.setDefaultStroke(new BasicStroke(lineThickness));
        ((AbstractRenderer) renderer).setAutoPopulateSeriesStroke(false);

        // Default update
        updateChartProperties(chart);
        return chart;
    }

    @JIPipeDocumentation(name = "Line thickness", description = "The thickness of the lines")
    @JIPipeParameter("line-thickness")
    public int getLineThickness() {
        return lineThickness;
    }

    @JIPipeParameter("line-thickness")
    public void setLineThickness(int lineThickness) {
        this.lineThickness = lineThickness;
    }

    public static LineCategoryPlotData importFrom(Path storagePath) {
        return PlotData.importFrom(storagePath, LineCategoryPlotData.class);
    }
}
