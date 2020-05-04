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

package org.hkijena.acaq5.extensions.plots.ui.plots;


import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.ui.events.PlotChangedEvent;
import org.hkijena.acaq5.ui.plotbuilder_old.*;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;

import java.util.List;

/**
 * A plot with categorical data
 */
public abstract class CategoryPlot extends ACAQLegacyPlot {

    private String categoryAxisLabel;
    private String valueAxisLabel;

    /**
     * @param seriesDataList the data
     */
    protected CategoryPlot(List<ACAQLegacyPlotSeriesData> seriesDataList) {
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
    protected ACAQLegacyPlotSeries createSeries() {
        ACAQLegacyPlotSeries series = new ACAQLegacyPlotSeries();
        series.addColumn("X", new ACAQLegacyStringPlotSeriesColumn(getSeriesDataList(),
                new ACAQLegacyPlotSeriesGenerator("No category", x -> "No category"),
                new ACAQLegacyPlotSeriesGenerator("Row number", x -> "x" + x)));
        series.addColumn("Category", new ACAQLegacyStringPlotSeriesColumn(getSeriesDataList(),
                new ACAQLegacyPlotSeriesGenerator("No category", x -> "No category")));
        series.addColumn("Value", new ACAQLegacyNumericPlotSeriesColumn(getSeriesDataList(),
                new ACAQLegacyPlotSeriesGenerator("Row index", x -> (double) x)));
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

    @ACAQDocumentation(name = "Category axis label", description = "Label of the category axis")
    @ACAQParameter("category-axis-label")
    public String getCategoryAxisLabel() {
        return categoryAxisLabel;
    }

    @ACAQParameter("category-axis-label")
    public void setCategoryAxisLabel(String categoryAxisLabel) {
        this.categoryAxisLabel = categoryAxisLabel;
        getEventBus().post(new PlotChangedEvent(this));
        getEventBus().post(new ParameterChangedEvent(this, "category-axis-label"));
    }

    @ACAQDocumentation(name = "Value axis label", description = "Label of the value axis")
    @ACAQParameter("value-axis-label")
    public String getValueAxisLabel() {
        return valueAxisLabel;
    }

    @ACAQParameter("value-axis-label")
    public void setValueAxisLabel(String valueAxisLabel) {
        this.valueAxisLabel = valueAxisLabel;
        getEventBus().post(new PlotChangedEvent(this));
        getEventBus().post(new ParameterChangedEvent(this, "value-axis-label"));
    }
}
