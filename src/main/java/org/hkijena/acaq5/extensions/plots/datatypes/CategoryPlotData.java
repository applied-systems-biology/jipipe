package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.utils.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains data for category plots.
 * Any category plot has following columns:
 * Value (Double), Category (String), Group (String)
 *
 * Values are assigned a category that is its X-axis.
 * Colors are assigned by its group
 */
public abstract class CategoryPlotData extends PlotData {

    private String categoryAxisLabel = "Category";
    private String valueAxisLabel = "Value";

    /**
     * Creates a new instance
     */
    public CategoryPlotData() {

    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public CategoryPlotData(CategoryPlotData other) {
        super(other);
        this.categoryAxisLabel = other.categoryAxisLabel;
        this.valueAxisLabel = other.valueAxisLabel;
    }

    /**
     * Creates a data set from the current series
     * @return the data set
     */
    public CategoryDataset createDataSet() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (PlotDataSeries series : getSeries()) {
            double[] values = series.getColumnAsDouble("Value");
            String[] categories = series.getColumnAsString("Category");
            String[] groups = series.getColumnAsString("Group");
            for (int i = 0; i < values.length; i++) {
                dataset.addValue(values[i], groups[i], categories[i]);
            }
        }
        return dataset;
    }

    @ACAQDocumentation(name = "Category axis label", description = "Label of the X-axis")
    @ACAQParameter("category-axis-label")
    public String getCategoryAxisLabel() {
        return categoryAxisLabel;
    }

    @ACAQParameter("category-axis-label")
    public void setCategoryAxisLabel(String categoryAxisLabel) {
        this.categoryAxisLabel = categoryAxisLabel;
        getEventBus().post(new ParameterChangedEvent(this, "category-axis-label"));
    }

    @ACAQDocumentation(name = "Value axis label", description = "Label of the Y-axis")
    @ACAQParameter("value-axis-label")
    public String getValueAxisLabel() {
        return valueAxisLabel;
    }

    @ACAQParameter("value-axis-label")
    public void setValueAxisLabel(String valueAxisLabel) {
        this.valueAxisLabel = valueAxisLabel;
        getEventBus().post(new ParameterChangedEvent(this, "value-axis-label"));
    }
}
