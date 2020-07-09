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

package org.hkijena.pipelinej.extensions.plots.datatypes;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

/**
 * Generates a bar category plot
 */
@ACAQDocumentation(name = "3D pie plot", description = "Plot that shows the amount for each category as slice in a pie.")
@PlotMetadata(columns = {@PlotColumn(name = "Amount", description = "The values to be displayed", isNumeric = true),
        @PlotColumn(name = "Category", description = "The categories to be displayed", isNumeric = false)})
public class Pie3DPlotData extends PiePlotData {

    /**
     * Creates a new instance
     */
    public Pie3DPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Pie3DPlotData(Pie3DPlotData other) {
        super(other);
    }

    @Override
    public JFreeChart getChart() {
        return ChartFactory.createPieChart3D(getTitle(), createDataSet(), true, true, false);
    }
}
