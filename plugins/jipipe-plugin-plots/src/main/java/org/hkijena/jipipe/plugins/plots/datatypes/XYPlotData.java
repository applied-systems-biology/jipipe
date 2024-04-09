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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
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
    private OptionalDoubleParameter xAxisMinimum = new OptionalDoubleParameter(Double.NEGATIVE_INFINITY, false);
    private OptionalDoubleParameter yAxisMinimum = new OptionalDoubleParameter(Double.NEGATIVE_INFINITY, false);
    private OptionalDoubleParameter xAxisMaximum = new OptionalDoubleParameter(Double.POSITIVE_INFINITY, false);
    private OptionalDoubleParameter yAxisMaximum = new OptionalDoubleParameter(Double.POSITIVE_INFINITY, false);

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
        this.xAxisMinimum = new OptionalDoubleParameter(other.xAxisMinimum);
        this.yAxisMinimum = new OptionalDoubleParameter(other.yAxisMinimum);
        this.xAxisMaximum = new OptionalDoubleParameter(other.xAxisMaximum);
        this.yAxisMaximum = new OptionalDoubleParameter(other.yAxisMaximum);
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

    @SetJIPipeDocumentation(name = "X axis label", description = "Label of the X-axis")
    @JIPipeParameter("x-axis-label")
    public String getxAxisLabel() {
        return xAxisLabel;
    }

    @JIPipeParameter("x-axis-label")
    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;

    }

    @SetJIPipeDocumentation(name = "Y axis label", description = "Label of the Y-axis")
    @JIPipeParameter("y-axis-label")
    public String getyAxisLabel() {
        return yAxisLabel;
    }

    @JIPipeParameter("y-axis-label")
    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;

    }

    @SetJIPipeDocumentation(name = "X axis font size", description = "Font size of the X axis")
    @JIPipeParameter("x-axis-font-size")
    public int getxAxisFontSize() {
        return xAxisFontSize;
    }

    @JIPipeParameter("x-axis-font-size")
    public void setxAxisFontSize(int xAxisFontSize) {
        this.xAxisFontSize = xAxisFontSize;
    }

    @SetJIPipeDocumentation(name = "Y axis font size", description = "Font size of the Y axis")
    @JIPipeParameter("y-axis-font-size")
    public int getyAxisFontSize() {
        return yAxisFontSize;
    }

    @JIPipeParameter("y-axis-font-size")
    public void setyAxisFontSize(int yAxisFontSize) {
        this.yAxisFontSize = yAxisFontSize;
    }

    @SetJIPipeDocumentation(name = "X axis minimum", description = "Minimum of the X axis values. If disabled or infinite, the value is calculated automatically.")
    @JIPipeParameter("x-axis-minimum")
    public OptionalDoubleParameter getxAxisMinimum() {
        return xAxisMinimum;
    }

    @JIPipeParameter("x-axis-minimum")
    public void setxAxisMinimum(OptionalDoubleParameter xAxisMinimum) {
        this.xAxisMinimum = xAxisMinimum;
    }

    @SetJIPipeDocumentation(name = "Y axis minimum", description = "Minimum of the Y axis values. If disabled or infinite, the value is calculated automatically.")
    @JIPipeParameter("y-axis-minimum")
    public OptionalDoubleParameter getyAxisMinimum() {
        return yAxisMinimum;
    }

    @JIPipeParameter("y-axis-minimum")
    public void setyAxisMinimum(OptionalDoubleParameter yAxisMinimum) {
        this.yAxisMinimum = yAxisMinimum;
    }

    @SetJIPipeDocumentation(name = "X axis maximum", description = "Maximum of the X axis values. If disabled or infinite, the value is calculated automatically.")
    @JIPipeParameter("x-axis-maximum")
    public OptionalDoubleParameter getxAxisMaximum() {
        return xAxisMaximum;
    }

    @JIPipeParameter("x-axis-maximum")
    public void setxAxisMaximum(OptionalDoubleParameter xAxisMaximum) {
        this.xAxisMaximum = xAxisMaximum;
    }

    @SetJIPipeDocumentation(name = "Y axis maximum", description = "Maximum of the Y axis values. If disabled or infinite, the value is calculated automatically.")
    @JIPipeParameter("y-axis-maximum")
    public OptionalDoubleParameter getyAxisMaximum() {
        return yAxisMaximum;
    }

    @JIPipeParameter("y-axis-maximum")
    public void setyAxisMaximum(OptionalDoubleParameter yAxisMaximum) {
        this.yAxisMaximum = yAxisMaximum;
    }
}
