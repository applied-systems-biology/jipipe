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

package org.hkijena.jipipe.extensions.plots.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

import java.nio.file.Path;

/**
 * Generates a bar category plot
 */
@JIPipeDocumentation(name = "XY scatter plot", description = "Plot that displays the Y values against the X values.")
@PlotMetadata(columns = {@PlotColumn(name = "X", description = "The X values", isNumeric = true),
        @PlotColumn(name = "Y", description = "The Y values", isNumeric = true)})
public class ScatterXYPlotData extends XYPlotData {

    /**
     * Creates a new instance
     */
    public ScatterXYPlotData() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ScatterXYPlotData(ScatterXYPlotData other) {
        super(other);
    }

    @Override
    public JFreeChart getChart() {
        return ChartFactory.createScatterPlot(getTitle(), getxAxisLabel(), getyAxisLabel(), createDataSet());
    }

    public static ScatterXYPlotData importFrom(Path storagePath) {
        return PlotData.importFrom(storagePath, ScatterXYPlotData.class);
    }
}
