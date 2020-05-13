package org.hkijena.acaq5.extensions.plots.datatypes;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.statistics.StatisticalCategoryDataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains data for statistical plots. Generates {@link StatisticalCategoryDataset}.
 * Any category plot has following columns:
 * Value (Double), Category (String), Group (String)
 * <p>
 * Values are assigned a category that is its X-axis.
 * Colors are assigned by its group
 */
public abstract class StatisticalCategoryPlotData extends CategoryPlotData {

    /**
     * Creates a new instance
     */
    public StatisticalCategoryPlotData() {
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public StatisticalCategoryPlotData(StatisticalCategoryPlotData other) {
        super(other);
    }

    @Override
    public CategoryDataset createDataSet() {
        DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();

        // Map from category -> group -> value
        Map<String, Map<String, List<Double>>> listMap = new HashMap<>();

        for (PlotDataSeries series : getSeries()) {
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

                double sum = 0;
                double sumSq = 0;
                for (double v : groupEntry.getValue()) {
                    sum += v;
                    sumSq += v * v;
                }

                double mean = sum / categoryEntry.getValue().size();
                double var = (sumSq / categoryEntry.getValue().size()) - mean * mean;

                dataset.add(mean, Math.sqrt(var), categoryEntry.getKey(), groupEntry.getKey());
            }
        }

        return dataset;
    }
}
