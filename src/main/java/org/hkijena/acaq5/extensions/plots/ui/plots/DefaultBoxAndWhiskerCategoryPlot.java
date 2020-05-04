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


import org.hkijena.acaq5.ui.plotbuilder_old.ACAQLegacyPlotSeries;
import org.hkijena.acaq5.ui.plotbuilder_old.ACAQLegacyPlotSeriesData;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for box plots
 */
public abstract class DefaultBoxAndWhiskerCategoryPlot extends CategoryPlot {
    private DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

    /**
     * @param seriesDataList the data
     */
    protected DefaultBoxAndWhiskerCategoryPlot(List<ACAQLegacyPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        addSeries();
    }

    @Override
    public CategoryDataset getDataset() {
        return dataset;
    }

    protected void updateDataset() {
        dataset.clear();
        ACAQLegacyPlotSeries series = getSeries().get(0);
        int rowCount = series.getAsNumericColumn("Value").getRequiredRowCount();
        List<String> xvalues = series.getAsStringColumn("X").getValues(rowCount);
        List<String> categories = series.getAsStringColumn("Category").getValues(rowCount);
        List<Double> values = series.getAsNumericColumn("Value").getValues(rowCount);

        Map<String, Map<String, List<Double>>> splitValues = new HashMap<>();

        for (int i = 0; i < values.size(); ++i) {
            String x = xvalues.get(i);
            String category = categories.get(i);
            double value = values.get(i);

            if (!splitValues.containsKey(x))
                splitValues.put(x, new HashMap<>());
            if (!splitValues.get(x).containsKey(category))
                splitValues.get(x).put(category, new ArrayList<>());

            splitValues.get(x).get(category).add(value);
        }

        for (Map.Entry<String, Map<String, List<Double>>> xentry : splitValues.entrySet()) {
            for (Map.Entry<String, List<Double>> categoryEntry : xentry.getValue().entrySet()) {
                dataset.add(categoryEntry.getValue(), categoryEntry.getKey(), xentry.getKey());
            }
        }
    }
}
