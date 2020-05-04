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
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;

import java.util.List;

/**
 * Statistical bar plot
 */
public class DefaultStatisticalBarCategoryPlot extends DefaultStatisticalCategoryPlot {

    /**
     * @param seriesDataList the data
     */
    public DefaultStatisticalBarCategoryPlot(List<ACAQPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        setTitle("Bar plot");
    }

    @Override
    protected JFreeChart createPlotFromDataset() {
        JFreeChart chart = ChartFactory.createBarChart(getTitle(), getCategoryAxisLabel(), getValueAxisLabel(), getDataset());
        chart.getCategoryPlot().setRenderer(new StatisticalBarRenderer());
        ((BarRenderer) chart.getCategoryPlot().getRenderer()).setBarPainter(new StandardBarPainter());
        return chart;
    }
}
