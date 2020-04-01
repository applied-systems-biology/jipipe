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


import org.hkijena.acaq5.ui.plotbuilder.*;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;

import java.util.List;

/**
 * A plot with categorical data
 */
public abstract class CategoryPlot extends ACAQPlot {

    private String categoryAxisLabel;
    private String valueAxisLabel;

    /**
     * @param seriesDataList the data
     */
    protected CategoryPlot(List<ACAQPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        addSeries();
    }


    @Override
    public boolean canRemoveSeries() {
        return false;
    }

    @Override
    public boolean canAddSeries() {
        return getSeries().size() == 0;
    }

    @Override
    protected ACAQPlotSeries createSeries() {
        ACAQPlotSeries series = new ACAQPlotSeries();
        series.addColumn("X", new ACAQStringPlotSeriesColumn(getSeriesDataList(),
                new ACAQPlotSeriesGenerator<>("No category", x -> "No category"),
                new ACAQPlotSeriesGenerator<>("Row number", x -> "x" + x)));
        series.addColumn("Category", new ACAQStringPlotSeriesColumn(getSeriesDataList(),
                new ACAQPlotSeriesGenerator<>("No category", x -> "No category")));
        series.addColumn("Value", new ACAQNumericPlotSeriesColumn(getSeriesDataList(),
                new ACAQPlotSeriesGenerator<>("Row index", x -> (double) x)));
        return series;
    }

    public abstract CategoryDataset getDataset();

    /**
     * Updates the data set
     */
    protected abstract void updateDataset();

    /**
     * Generates the chart
     *
     * @return the chart
     */
    protected abstract JFreeChart createPlotFromDataset();

    @Override
    public final JFreeChart createPlot() {
        updateDataset();
        return createPlotFromDataset();
    }

    public String getCategoryAxisLabel() {
        return categoryAxisLabel;
    }

    public void setCategoryAxisLabel(String categoryAxisLabel) {
        this.categoryAxisLabel = categoryAxisLabel;
        getEventBus().post(new PlotChangedEvent(this));
    }

    public String getValueAxisLabel() {
        return valueAxisLabel;
    }

    public void setValueAxisLabel(String valueAxisLabel) {
        this.valueAxisLabel = valueAxisLabel;
        getEventBus().post(new PlotChangedEvent(this));
    }
}
