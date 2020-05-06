package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

/**
 * Contains data for pie plots.
 * Any pie plot has following columns:
 * Amount (Double), Category (String)
 */
public abstract class PiePlotData extends PlotData {

    /**
     * Creates a new instance
     */
    public PiePlotData() {

    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PiePlotData(PiePlotData other) {
        super(other);
    }

    /**
     * Creates a data set from the current series
     * @return the data set
     */
    public PieDataset createDataSet() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (PlotDataSeries series : getSeries()) {
            double[] values = series.getColumnAsDouble("Value");
            String[] categories = series.getColumnAsString("Category");
            for (int i = 0; i < values.length; i++) {
                dataset.setValue(categories[i], values[i]);
            }
        }
        return dataset;
    }
}
