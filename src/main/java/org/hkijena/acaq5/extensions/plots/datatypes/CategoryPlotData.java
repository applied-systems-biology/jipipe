/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
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
     *
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

    }

    @ACAQDocumentation(name = "Value axis label", description = "Label of the Y-axis")
    @ACAQParameter("value-axis-label")
    public String getValueAxisLabel() {
        return valueAxisLabel;
    }

    @ACAQParameter("value-axis-label")
    public void setValueAxisLabel(String valueAxisLabel) {
        this.valueAxisLabel = valueAxisLabel;

    }
}
