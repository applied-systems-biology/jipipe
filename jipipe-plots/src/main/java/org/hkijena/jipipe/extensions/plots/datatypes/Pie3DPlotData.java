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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;

import java.awt.Font;
import java.nio.file.Path;

/**
 * Generates a bar category plot
 */
@JIPipeDocumentation(name = "3D pie plot", description = "Plot that shows the amount for each category as slice in a pie.")
@PlotMetadata(columns = {@PlotColumn(name = "Amount", description = "The values to be displayed", isNumeric = true),
        @PlotColumn(name = "Category", description = "The categories to be displayed", isNumeric = false)})
public class Pie3DPlotData extends PiePlotData {

    private int labelFontSize = 12;

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
        this.labelFontSize = other.labelFontSize;
    }

    @Override
    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createPieChart3D(getTitle(), createDataSet(), true, true, false);
        PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setLabelBackgroundPaint(getBackgroundColor());
        plot.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, labelFontSize));
        updateChartProperties(chart);
        return chart;
    }

    @JIPipeDocumentation(name = "Label font size", description = "Font size of the pie chart labels")
    @JIPipeParameter("label-font-size")
    public int getLabelFontSize() {
        return labelFontSize;
    }

    @JIPipeParameter("label-font-size")
    public boolean setLabelFontSize(int labelFontSize) {
        if (labelFontSize <= 0)
            return false;
        this.labelFontSize = labelFontSize;
        return true;
    }

    public static Pie3DPlotData importFrom(Path storagePath) {
        return PlotData.importFrom(storagePath, Pie3DPlotData.class);
    }
}
