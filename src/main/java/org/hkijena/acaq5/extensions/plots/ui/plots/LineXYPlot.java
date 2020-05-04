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


import org.hkijena.acaq5.ui.plotbuilder.ACAQPlotSeriesData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.List;

/**
 * An XY plot
 */
public class LineXYPlot extends XYPlot {
    /**
     * @param seriesDataList the data
     */
    public LineXYPlot(List<ACAQPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        setTitle("XY Line Plot");
    }

    @Override
    protected JFreeChart createPlotFromDataset(XYSeriesCollection dataset) {
        return ChartFactory.createXYLineChart(getTitle(), getxAxisLabel(), getyAxisLabel(), dataset);
    }
}
