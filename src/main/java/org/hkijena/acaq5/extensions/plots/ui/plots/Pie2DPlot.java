/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.extensions.plots.ui.plots;


import org.hkijena.acaq5.ui.plotbuilder_old.ACAQLegacyPlotSeriesData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import java.util.List;

/**
 * A 2D pie plot
 */
@Deprecated
public class Pie2DPlot extends PiePlot {
    /**
     * @param seriesDataList the data
     */
    public Pie2DPlot(List<ACAQLegacyPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        setTitle("2D Pie plot");
    }

    @Override
    protected JFreeChart createPlotFromDataset(DefaultPieDataset dataset) {
        return ChartFactory.createPieChart(getTitle(), dataset, true, true, false);
    }
}
