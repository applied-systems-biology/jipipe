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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

/**
 * Generates a bar category plot
 */
@ACAQDocumentation(name = "Box plot", description = "Box and whisker plot.")
@PlotMetadata(columns = {@PlotColumn(name = "Value", description = "The values", isNumeric = true),
        @PlotColumn(name = "Category", description = "Category for each value. Displayed in the X axis.", isNumeric = false),
        @PlotColumn(name = "Group", description = "Group for each value. Bars are colored by this column.", isNumeric = false)})
public class BarBoxAndWhiskerCategoryPlotData extends BoxAndWhiskerCategoryPlotData {

    /**
     * Creates a new instance
     */
    public BarBoxAndWhiskerCategoryPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public BarBoxAndWhiskerCategoryPlotData(BarBoxAndWhiskerCategoryPlotData other) {
        super(other);
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(getTitle(),
                getCategoryAxisLabel(),
                getValueAxisLabel(),
                (BoxAndWhiskerCategoryDataset) createDataSet(),
                true);
        CustomBoxAndWhiskerRenderer renderer = new CustomBoxAndWhiskerRenderer();
        chart.getCategoryPlot().setRenderer(renderer);
        return chart;
    }
}
