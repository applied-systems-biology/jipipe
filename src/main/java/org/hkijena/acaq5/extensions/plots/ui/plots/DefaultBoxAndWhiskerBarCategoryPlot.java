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
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

import java.util.List;

/**
 * A box plot
 */
public class DefaultBoxAndWhiskerBarCategoryPlot extends DefaultBoxAndWhiskerCategoryPlot {

    /**
     * @param seriesDataList the data
     */
    public DefaultBoxAndWhiskerBarCategoryPlot(List<ACAQPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        setTitle("Box Plot");
    }

    @Override
    protected JFreeChart createPlotFromDataset() {
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), (BoxAndWhiskerCategoryDataset) getDataset(), true);
        CustomBoxAndWhiskerRenderer renderer = new CustomBoxAndWhiskerRenderer();
        chart.getCategoryPlot().setRenderer(renderer);
        return chart;
    }
}
