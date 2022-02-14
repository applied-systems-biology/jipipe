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
import org.jfree.chart.renderer.xy.XYItemRenderer;

import java.awt.*;
import java.nio.file.Path;

/**
 * Generates a bar category plot
 * Series table columns: X (Double), Y (Double)
 * Multiple series: Yes
 */
@JIPipeDocumentation(name = "XY line plot", description = "Plot that displays the Y values against the X values.")
@PlotMetadata(columns = {@PlotColumn(name = "X", description = "The X values", isNumeric = true),
        @PlotColumn(name = "Y", description = "The Y values", isNumeric = true)}, maxSeriesCount = Integer.MAX_VALUE)
public class LineXYPlotData extends XYPlotData {

    private int lineThickness = 1;

    /**
     * Creates a new instance
     */
    public LineXYPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public LineXYPlotData(LineXYPlotData other) {
        super(other);
        this.lineThickness = other.lineThickness;
    }

    public static LineXYPlotData importFrom(Path storagePath) {
        return PlotData.importFrom(storagePath, LineXYPlotData.class);
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createXYLineChart(getTitle(), getxAxisLabel(), getyAxisLabel(), createDataSet());
        chart.getXYPlot().setDomainGridlinePaint(getGridColor());
        chart.getXYPlot().getDomainAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getxAxisFontSize()));
        chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getxAxisFontSize()));
        chart.getXYPlot().getRangeAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getyAxisFontSize()));
        chart.getXYPlot().getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getyAxisFontSize()));

        calibrateAxis(chart.getXYPlot().getDomainAxis(), getxAxisMinimum(), getxAxisMaximum());
        calibrateAxis(chart.getXYPlot().getRangeAxis(), getyAxisMinimum(), getyAxisMaximum());

        // Set line thickness
        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        renderer.setDefaultStroke(new BasicStroke(lineThickness));
        ((AbstractRenderer) renderer).setAutoPopulateSeriesStroke(false);

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
}
