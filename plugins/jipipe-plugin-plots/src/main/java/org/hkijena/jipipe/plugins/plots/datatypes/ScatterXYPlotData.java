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

import java.awt.*;

/**
 * A scatter XY plot
 * Series table columns: X (Double), Y (Double)
 * Multiple series: Yes
 */
@SetJIPipeDocumentation(name = "XY scatter plot", description = "Plot that displays the Y values against the X values.")
@PlotMetadata(columns = {@PlotColumn(name = "X", description = "The X values", isNumeric = true),
        @PlotColumn(name = "Y", description = "The Y values", isNumeric = true)}, maxSeriesCount = Integer.MAX_VALUE)
public class ScatterXYPlotData extends XYPlotData {

    /**
     * Creates a new instance
     */
    public ScatterXYPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ScatterXYPlotData(ScatterXYPlotData other) {
        super(other);
    }

    public static ScatterXYPlotData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return PlotData.importData(storage, ScatterXYPlotData.class, progressInfo);
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createScatterPlot(getTitle(), getxAxisLabel(), getyAxisLabel(), createDataSet());
        chart.getXYPlot().setDomainGridlinePaint(getGridColor());
        chart.getXYPlot().getDomainAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getxAxisFontSize()));
        chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getxAxisFontSize()));
        chart.getXYPlot().getRangeAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, getyAxisFontSize()));
        chart.getXYPlot().getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, getyAxisFontSize()));

        calibrateAxis(chart.getXYPlot().getDomainAxis(), getxAxisMinimum(), getxAxisMaximum());
        calibrateAxis(chart.getXYPlot().getRangeAxis(), getyAxisMinimum(), getyAxisMaximum());

        updateChartProperties(chart);
        return chart;
    }
}
