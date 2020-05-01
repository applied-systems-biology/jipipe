/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.extensions.standardplots.ui.plots;


import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.ui.events.PlotChangedEvent;
import org.hkijena.acaq5.ui.plotbuilder.*;
import org.hkijena.acaq5.utils.StringUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A XY point plot
 */
public abstract class XYPlot extends ACAQPlot {

    private String xAxisLabel = "X";
    private String yAxisLabel = "Y";
    private XYSeriesCollection dataset = new XYSeriesCollection();

    /**
     * @param seriesDataList the data
     */
    public XYPlot(List<ACAQPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        addSeries();
    }

    @Override
    public boolean canRemoveSeries() {
        return series.size() > 1;
    }

    @Override
    public boolean canAddSeries() {
        return true;
    }

    @Override
    protected ACAQPlotSeries createSeries() {
        ACAQPlotSeries series = new ACAQPlotSeries();
        series.addParameter("Name", "Series");
        series.addColumn("X", new ACAQNumericPlotSeriesColumn(getSeriesDataList(),
                new ACAQPlotSeriesGenerator<>("Row number", x -> (double) x)));
        series.addColumn("Y", new ACAQNumericPlotSeriesColumn(getSeriesDataList(),
                new ACAQPlotSeriesGenerator<>("Row number", x -> (double) x)));
        return series;
    }

    /**
     * @param dataset the data set
     * @return the plot
     */
    protected abstract JFreeChart createPlotFromDataset(XYSeriesCollection dataset);

    /**
     * Updates the data set
     */
    protected void updateDataset() {
        dataset.removeAllSeries();
        Set<String> existingSeries = new HashSet<>();
        for (ACAQPlotSeries seriesEntry : series) {
            if (!seriesEntry.isEnabled())
                continue;
            String name = StringUtils.makeUniqueString(seriesEntry.getParameterValue("Name").toString(), " ", existingSeries);
            XYSeries chartSeries = new XYSeries(name, true);

            int rowCount = seriesEntry.getMaximumRequiredRowCount();

            List<Double> xValues = ((ACAQNumericPlotSeriesColumn) seriesEntry.getColumns().get("X")).getValues(rowCount);
            List<Double> yValues = ((ACAQNumericPlotSeriesColumn) seriesEntry.getColumns().get("Y")).getValues(rowCount);
            for (int i = 0; i < xValues.size(); ++i) {
                chartSeries.add(xValues.get(i), yValues.get(i));
            }
            dataset.addSeries(chartSeries);
            existingSeries.add(name);
        }
    }

    @Override
    public final JFreeChart createPlot() {
        updateDataset();
        return createPlotFromDataset(dataset);
    }

    @ACAQDocumentation(name = "X axis label", description = "Label of the X axis")
    @ACAQParameter("x-axis-label")
    public String getxAxisLabel() {
        return xAxisLabel;
    }

    @ACAQParameter("x-axis-label")
    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
        getEventBus().post(new PlotChangedEvent(this));
    }

    @ACAQDocumentation(name = "Y axis label", description = "Label of the Y axis")
    @ACAQParameter("y-axis-label")
    public String getyAxisLabel() {
        return yAxisLabel;
    }

    @ACAQParameter("y-axis-label")
    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
        getEventBus().post(new PlotChangedEvent(this));
    }

    public XYSeriesCollection getDataset() {
        return dataset;
    }
}
