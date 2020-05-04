package org.hkijena.acaq5.extensions.plots.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.utils.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains data to generate a histogram plot
 */
@ACAQDocumentation(name = "Histogram plot", description = "Bar chart that displays the number of items for each bin. Please note that this plot requires " +
        "raw values as input. A pre-defined histogram table should be rendered with an XY bar plot.")
@PlotMetadata(columns = { @PlotColumn(name = "Value", dataType = Double.class) })
public class HistogramPlotData extends PlotData {

    private String binAxisLabel = "Bin";
    private String valueAxisLabel = "Value";
    private int bins = 10;

    /**
     * Creates a new instance
     */
    public HistogramPlotData() {
        setTitle("Histogram plot");
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public HistogramPlotData(HistogramPlotData other) {
        super(other);
        this.binAxisLabel = other.binAxisLabel;
        this.valueAxisLabel = other.valueAxisLabel;
    }

    @Override
    public JFreeChart getChart() {
        Set<String> existingSeriesNames = new HashSet<>();
        HistogramDataset dataset = new HistogramDataset();
        for (PlotDataSeries series : getSeries()) {
            String name = StringUtils.isNullOrEmpty(series.getName()) ? "Series" : series.getName();
            name = StringUtils.makeUniqueString(name, " ", existingSeriesNames);
            existingSeriesNames.add(name);

            dataset.addSeries(name, series.getColumnAsDouble("Value"), bins);
        }
        JFreeChart chart = ChartFactory.createHistogram(getTitle(), getBinAxisLabel(), getValueAxisLabel(), dataset);
        ((XYBarRenderer) chart.getXYPlot().getRenderer()).setBarPainter(new StandardXYBarPainter());
        return chart;
    }

    @ACAQDocumentation(name = "Bin axis label", description = "Label of the X-axis")
    @ACAQParameter("bin-axis-label")
    public String getBinAxisLabel() {
        return binAxisLabel;
    }

    @ACAQParameter("bin-axis-label")
    public void setBinAxisLabel(String binAxisLabel) {
        this.binAxisLabel = binAxisLabel;
        getEventBus().post(new ParameterChangedEvent(this, "bin-axis-label"));
    }

    @ACAQDocumentation(name = "Value axis label", description = "Label of the Y-axis")
    @ACAQParameter("value-axis-label")
    public String getValueAxisLabel() {
        return valueAxisLabel;
    }

    @ACAQParameter("value-axis-label")
    public void setValueAxisLabel(String valueAxisLabel) {
        this.valueAxisLabel = valueAxisLabel;
        getEventBus().post(new ParameterChangedEvent(this, "value-axis-label"));
    }

    @ACAQDocumentation(name = "Bins", description = "Number of bins")
    @ACAQParameter("bins")
    public int getBins() {
        return bins;
    }

    @ACAQParameter("bins")
    public void setBins(int bins) {
        this.bins = bins;
    }
}
