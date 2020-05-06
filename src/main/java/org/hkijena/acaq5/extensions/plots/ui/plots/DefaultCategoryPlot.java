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
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.List;

/**
 * Default implementation for categorical plots
 */
@Deprecated
public abstract class DefaultCategoryPlot extends CategoryPlot {

    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    /**
     * @param seriesDataList the data
     */
    public DefaultCategoryPlot(List<ACAQLegacyPlotSeriesData> seriesDataList) {
        super(seriesDataList);
    }

    @Override
    protected void updateDataset() {
        dataset.clear();
        ACAQLegacyPlotSeries series = getSeries().get(0);
        int rowCount = series.getMaximumRequiredRowCount();
        List<String> xvalues = series.getAsStringColumn("X").getValues(rowCount);
        List<String> categories = series.getAsStringColumn("Category").getValues(rowCount);
        List<Double> values = series.getAsNumericColumn("Value").getValues(rowCount);
        for (int i = 0; i < xvalues.size(); ++i) {
            dataset.addValue(values.get(i), categories.get(i), xvalues.get(i));
        }
    }

    @Override
    public DefaultCategoryDataset getDataset() {
        return dataset;
    }
}
