package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.utils.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
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
        getEventBus().post(new ParameterChangedEvent(this, "x-axis-label"));
    }

    @ACAQDocumentation(name = "Y axis label", description = "Label of the Y-axis")
    @ACAQParameter("y-axis-label")
    public String getyAxisLabel() {
        return yAxisLabel;
    }

    @ACAQParameter("y-axis-label")
    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
        getEventBus().post(new ParameterChangedEvent(this, "y-axis-label"));
    }
}
