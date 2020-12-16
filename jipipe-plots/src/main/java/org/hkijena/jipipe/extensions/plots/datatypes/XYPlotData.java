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
import org.hkijena.jipipe.utils.StringUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains data for XY plots
 * Any plot has following columns:
 * X (Double), Y (Double)
 */
public abstract class XYPlotData extends PlotData {

    private String xAxisLabel = "X";
    private String yAxisLabel = "Y";
    private int xAxisFontSize = 12;
    private int yAxisFontSize = 12;

    /**
     * Creates a new instance
     */
    public XYPlotData() {

    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public XYPlotData(XYPlotData other) {
        super(other);
        this.xAxisLabel = other.xAxisLabel;
        this.yAxisLabel = other.yAxisLabel;
        this.xAxisFontSize = other.xAxisFontSize;
        this.yAxisFontSize = other.yAxisFontSize;
    }

    /**
     * Creates a data set from the current series
     *
     * @return the data set
     */
    public XYDataset createDataSet() {
        Set<String> existingSeriesNames = new HashSet<>();
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (PlotDataSeries series : getSeries()) {
            String name = StringUtils.isNullOrEmpty(series.getName()) ? "Series" : series.getName();
            name = StringUtils.makeUniqueString(name, " ", existingSeriesNames);
            existingSeriesNames.add(name);

            double[] xValues = series.getColumnAsDouble("X");
            double[] yValues = series.getColumnAsDouble("Y");

            XYSeries xySeries = new XYSeries(name, true);
            for (int i = 0; i < xValues.length; i++) {
                xySeries.add(xValues[i], yValues[i]);
            }
            dataset.addSeries(xySeries);
        }
        return dataset;
    }

    @JIPipeDocumentation(name = "X axis label", description = "Label of the X-axis")
    @JIPipeParameter("x-axis-label")
    public String getxAxisLabel() {
        return xAxisLabel;
    }

    @JIPipeParameter("x-axis-label")
    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;

    }

    @JIPipeDocumentation(name = "Y axis label", description = "Label of the Y-axis")
    @JIPipeParameter("y-axis-label")
    public String getyAxisLabel() {
        return yAxisLabel;
    }

    @JIPipeParameter("y-axis-label")
    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;

    }

    @JIPipeDocumentation(name = "X axis font size", description = "Font size of the X axis")
    @JIPipeParameter("x-axis-font-size")
    public int getxAxisFontSize() {
        return xAxisFontSize;
    }

    @JIPipeParameter("x-axis-font-size")
    public void setxAxisFontSize(int xAxisFontSize) {
        this.xAxisFontSize = xAxisFontSize;
    }

    @JIPipeDocumentation(name = "Y axis font size", description = "Font size of the Y axis")
    @JIPipeParameter("y-axis-font-size")
    public int getyAxisFontSize() {
        return yAxisFontSize;
    }

    @JIPipeParameter("y-axis-font-size")
    public void setyAxisFontSize(int yAxisFontSize) {
        this.yAxisFontSize = yAxisFontSize;
    }
}
