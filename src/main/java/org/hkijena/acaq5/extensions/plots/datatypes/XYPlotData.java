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

package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.utils.StringUtils;
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

    @ACAQDocumentation(name = "X axis label", description = "Label of the X-axis")
    @ACAQParameter("x-axis-label")
    public String getxAxisLabel() {
        return xAxisLabel;
    }

    @ACAQParameter("x-axis-label")
    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;

    }

    @ACAQDocumentation(name = "Y axis label", description = "Label of the Y-axis")
    @ACAQParameter("y-axis-label")
    public String getyAxisLabel() {
        return yAxisLabel;
    }

    @ACAQParameter("y-axis-label")
    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;

    }
}
