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
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;

import java.util.List;

/**
 * A line category plot
 */
public class DefaultStatisticalLineCategoryPlot extends DefaultStatisticalCategoryPlot {

    /**
     * @param seriesDataList the data
     */
    public DefaultStatisticalLineCategoryPlot(List<ACAQPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        setTitle("Line category plot");
    }

    @Override
    protected JFreeChart createPlotFromDataset() {
        JFreeChart chart = ChartFactory.createLineChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), getDataset());
        chart.getCategoryPlot().setRenderer(new StatisticalLineAndShapeRenderer());
        return chart;
    }
}
