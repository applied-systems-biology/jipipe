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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Contains data for category plots.
 * Any category plot has following columns:
 * Value (Double), Category (String), Group (String)
 * <p>
 * Values are assigned a category that is its X-axis.
 * Colors are assigned by its group
 */
public abstract class CategoryJFreeChartPlotData extends JFreeChartPlotData {

    private String categoryAxisLabel = "Category";
    private String valueAxisLabel = "Value";
    private int categoryAxisFontSize = 12;
    private int valueAxisFontSize = 12;
    private OptionalDoubleParameter valueAxisMinimum = new OptionalDoubleParameter(Double.NEGATIVE_INFINITY, false);
    private OptionalDoubleParameter valueAxisMaximum = new OptionalDoubleParameter(Double.POSITIVE_INFINITY, false);

    /**
     * Creates a new instance
     */
    public CategoryJFreeChartPlotData() {

    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public CategoryJFreeChartPlotData(CategoryJFreeChartPlotData other) {
        super(other);
        this.categoryAxisLabel = other.categoryAxisLabel;
        this.valueAxisLabel = other.valueAxisLabel;
        this.categoryAxisFontSize = other.categoryAxisFontSize;
        this.valueAxisFontSize = other.valueAxisFontSize;
        this.valueAxisMinimum = new OptionalDoubleParameter(other.valueAxisMinimum);
        this.valueAxisMaximum = new OptionalDoubleParameter(other.valueAxisMaximum);
    }

    /**
     * Creates a data set from the current series
     *
     * @return the data set
     */
    public CategoryDataset createDataSet() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (JFreeChartPlotDataSeries series : getSeries()) {
            double[] values = series.getColumnAsDouble("Value");
            String[] categories = series.getColumnAsString("Category");
            String[] groups = series.getColumnAsString("Group");
            for (int i = 0; i < values.length; i++) {
                dataset.addValue(values[i], groups[i], categories[i]);
            }
        }
        return dataset;
    }

    @SetJIPipeDocumentation(name = "Category axis label", description = "Label of the X-axis")
    @JIPipeParameter("category-axis-label")
    public String getCategoryAxisLabel() {
        return categoryAxisLabel;
    }

    @JIPipeParameter("category-axis-label")
    public void setCategoryAxisLabel(String categoryAxisLabel) {
        this.categoryAxisLabel = categoryAxisLabel;

    }

    @SetJIPipeDocumentation(name = "Value axis label", description = "Label of the Y-axis")
    @JIPipeParameter("value-axis-label")
    public String getValueAxisLabel() {
        return valueAxisLabel;
    }

    @JIPipeParameter("value-axis-label")
    public void setValueAxisLabel(String valueAxisLabel) {
        this.valueAxisLabel = valueAxisLabel;
    }

    @SetJIPipeDocumentation(name = "Category axis font size", description = "Font size of the category axis")
    @JIPipeParameter("category-axis-font-size")
    public int getCategoryAxisFontSize() {
        return categoryAxisFontSize;
    }

    @JIPipeParameter("category-axis-font-size")
    public void setCategoryAxisFontSize(int categoryAxisFontSize) {
        this.categoryAxisFontSize = categoryAxisFontSize;
    }

    @SetJIPipeDocumentation(name = "Value axis font size", description = "Font size of the value axis")
    @JIPipeParameter("value-axis-font-size")
    public int getValueAxisFontSize() {
        return valueAxisFontSize;
    }

    @JIPipeParameter("value-axis-font-size")
    public void setValueAxisFontSize(int valueAxisFontSize) {
        this.valueAxisFontSize = valueAxisFontSize;
    }

    @SetJIPipeDocumentation(name = "Value axis minimum", description = "Minimum of the value axis values. If disabled or infinite, the value is calculated automatically.")
    @JIPipeParameter("value-axis-minimum")
    public OptionalDoubleParameter getValueAxisMinimum() {
        return valueAxisMinimum;
    }

    @JIPipeParameter("value-axis-minimum")
    public void setValueAxisMinimum(OptionalDoubleParameter valueAxisMinimum) {
        this.valueAxisMinimum = valueAxisMinimum;
    }

    @SetJIPipeDocumentation(name = "Value axis maximum", description = "Maximum of the value axis values. If disabled or infinite, the value is calculated automatically.")
    @JIPipeParameter("value-axis-maximum")
    public OptionalDoubleParameter getValueAxisMaximum() {
        return valueAxisMaximum;
    }

    @JIPipeParameter("value-axis-maximum")
    public void setValueAxisMaximum(OptionalDoubleParameter valueAxisMaximum) {
        this.valueAxisMaximum = valueAxisMaximum;
    }
}
