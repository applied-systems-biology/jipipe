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


import org.hkijena.acaq5.ui.plotbuilder_old.*;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import java.util.List;

/**
 * Base class for pie plots
 * Expects data "Category" (String) and "Amount" (Numeric)
 */
public abstract class PiePlot extends ACAQPlot {

    private DefaultPieDataset dataset = new DefaultPieDataset();

    /**
     * @param seriesDataList the data
     */
    public PiePlot(List<ACAQPlotSeriesData> seriesDataList) {
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
        series.addColumn("Category", new ACAQStringPlotSeriesColumn(getSeriesDataList(),
                new ACAQPlotSeriesGenerator("No category", x -> "No category")));
        series.addColumn("Amount", new ACAQNumericPlotSeriesColumn(getSeriesDataList(),
                new ACAQPlotSeriesGenerator("Zero", x -> 0.0)));
        return series;
    }

    public DefaultPieDataset getDataset() {
        return dataset;
    }

    /**
     * Updates the data set
     */
    protected void updateDataset() {
        dataset.clear();
        ACAQPlotSeries series = getSeries().get(0);
        int rowCount = series.getMaximumRequiredRowCount();
        List<String> categories = ((ACAQStringPlotSeriesColumn) series.getColumns().get("Category")).getValues(rowCount);
        List<Double> values = ((ACAQNumericPlotSeriesColumn) series.getColumns().get("Amount")).getValues(rowCount);

        for (int i = 0; i < categories.size(); ++i) {
            dataset.setValue(categories.get(i), 0);
        }
        for (int i = 0; i < categories.size(); ++i) {
            dataset.setValue(categories.get(i), dataset.getValue(categories.get(i)).doubleValue() + values.get(i));
        }
    }

    /**
     * Creates the plot
     *
     * @param dataset the dataset
     * @return the plot
     */
    protected abstract JFreeChart createPlotFromDataset(DefaultPieDataset dataset);

    @Override
    public final JFreeChart createPlot() {
        updateDataset();
        return createPlotFromDataset(dataset);
    }
}
