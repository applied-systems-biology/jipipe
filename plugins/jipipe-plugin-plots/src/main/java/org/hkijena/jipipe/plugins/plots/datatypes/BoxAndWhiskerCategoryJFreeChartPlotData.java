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

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains data for box and whisker category plots category plots.
 * Any category plot has following columns:
 * Value (Double), Category (String), Group (String)
 * <p>
 * Values are assigned a category that is its X-axis.
 * Colors are assigned by its group
 */
public abstract class BoxAndWhiskerCategoryJFreeChartPlotData extends CategoryJFreeChartPlotData {

    /**
     * Creates a new instance
     */
    public BoxAndWhiskerCategoryJFreeChartPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public BoxAndWhiskerCategoryJFreeChartPlotData(BoxAndWhiskerCategoryJFreeChartPlotData other) {
        super(other);
    }

    @Override
    public CategoryDataset createDataSet() {
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

        // Map from category -> group -> value
        Map<String, Map<String, List<Double>>> listMap = new HashMap<>();

        for (JFreeChartPlotDataSeries series : getSeries()) {
            double[] values = series.getColumnAsDouble("Value");
            String[] categories = series.getColumnAsString("Category");
            String[] groups = series.getColumnAsString("Group");

            for (int i = 0; i < values.length; i++) {
                double value = values[i];
                String category = categories[i];
                String group = groups[i];

                Map<String, List<Double>> groupMap = listMap.getOrDefault(category, null);
                if (groupMap == null) {
                    groupMap = new HashMap<>();
                    listMap.put(category, groupMap);
                }

                List<Double> valueList = groupMap.getOrDefault(group, null);
                if (valueList == null) {
                    valueList = new ArrayList<>();
                    groupMap.put(group, valueList);
                }

                valueList.add(value);
            }
        }

        for (Map.Entry<String, Map<String, List<Double>>> categoryEntry : listMap.entrySet()) {
            for (Map.Entry<String, List<Double>> groupEntry : categoryEntry.getValue().entrySet()) {
                dataset.add(groupEntry.getValue(), groupEntry.getKey(), categoryEntry.getKey());
            }
        }

        return dataset;
    }
}
