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

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

/**
 * Contains data for pie plots.
 * Any pie plot has following columns:
 * Amount (Double), Category (String)
 */
public abstract class PieJFreeChartPlotData extends JFreeChartPlotData {

    /**
     * Creates a new instance
     */
    public PieJFreeChartPlotData() {

    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PieJFreeChartPlotData(PieJFreeChartPlotData other) {
        super(other);
    }

    /**
     * Creates a data set from the current series
     *
     * @return the data set
     */
    public PieDataset createDataSet() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (JFreeChartPlotDataSeries series : getSeries()) {
            double[] values = series.getColumnAsDouble("Amount");
            String[] categories = series.getColumnAsString("Category");
            for (int i = 0; i < values.length; i++) {
                dataset.setValue(categories[i], values[i]);
            }
        }
        return dataset;
    }
}
