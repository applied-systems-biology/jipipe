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

import com.google.common.primitives.Doubles;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.ui.events.PlotChangedEvent;
import org.hkijena.acaq5.ui.plotbuilder_old.*;
import org.hkijena.acaq5.utils.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Plot that shows a histogram
 */
@Deprecated
public class HistogramPlot extends ACAQLegacyPlot {

    private String xAxisLabel = "Bin";
    private String yAxisLabel = "Number";

    /**
     * @param seriesDataList the data
     */
    public HistogramPlot(List<ACAQLegacyPlotSeriesData> seriesDataList) {
        super(seriesDataList);
        setTitle("Histogram");
        addSeries();
    }

    @Override
    protected ACAQLegacyPlotSeries createSeries() {
        ACAQLegacyPlotSeries series = new ACAQLegacyPlotSeries();
        series.addParameter("Name", "Category");
        series.addParameter("Bins", 10);
        series.addColumn("Values", new ACAQLegacyNumericPlotSeriesColumn(getSeriesDataList(),
                new ACAQLegacyPlotSeriesGenerator("Zero", x -> 0.0)));
        return series;
    }

    @Override
    public JFreeChart createPlot() {
        HistogramDataset dataset = new HistogramDataset();
        Set<String> existingNames = new HashSet<>();
        for (ACAQLegacyPlotSeries seriesEntry : series) {
            if (!seriesEntry.isEnabled())
                continue;
            int rowCount = Math.max(1, seriesEntry.getMaximumRequiredRowCount());
            List<Double> values = seriesEntry.getAsNumericColumn("Values").getValues(rowCount);
            String name = StringUtils.makeUniqueString((String) seriesEntry.getParameterValue("Name"), " ", existingNames);
            dataset.addSeries(name, Doubles.toArray(values), (int) seriesEntry.getParameterValue("Bins"));
        }

        JFreeChart chart = ChartFactory.createHistogram(getTitle(), getxAxisLabel(), getyAxisLabel(), dataset);
        ((XYBarRenderer) chart.getXYPlot().getRenderer()).setBarPainter(new StandardXYBarPainter());
        return chart;
    }

    @ACAQDocumentation(name = "X axis label", description = "Label of the X axis")
    @ACAQParameter("x-axis-label")
    public String getxAxisLabel() {
        return xAxisLabel;
    }

    @ACAQParameter("x-axis-label")
    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
        getEventBus().post(new PlotChangedEvent(this));
    }

    @ACAQDocumentation(name = "Y axis label", description = "Label of the Y axis")
    @ACAQParameter("y-axis-label")
    public String getyAxisLabel() {
        return yAxisLabel;
    }

    @ACAQParameter("y-axis-label")
    public void setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
        getEventBus().post(new PlotChangedEvent(this));
    }

}
