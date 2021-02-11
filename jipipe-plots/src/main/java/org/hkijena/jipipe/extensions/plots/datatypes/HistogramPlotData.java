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
import org.hkijena.jipipe.utils.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import java.awt.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains data to generate a histogram plot
 */
@JIPipeDocumentation(name = "Histogram plot", description = "Bar chart that displays the number of items for each bin. Please note that this plot requires " +
        "raw values as input. A pre-defined histogram table should be rendered with an XY bar plot.")
@PlotMetadata(columns = {@PlotColumn(name = "Value", description = "Values to generate a histogram from.", isNumeric = true)})
public class HistogramPlotData extends PlotData {

    private String binAxisLabel = "Bin";
    private String valueAxisLabel = "Value";
    private int bins = 10;
    private HistogramType_ histogramType = HistogramType_.Frequency;
    private int binAxisFontSize = 12;
    private int valueAxisFontSize = 12;

    /**
     * Creates a new instance
     */
    public HistogramPlotData() {
        setTitle("Histogram plot");
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public HistogramPlotData(HistogramPlotData other) {
        super(other);
        this.binAxisLabel = other.binAxisLabel;
        this.valueAxisLabel = other.valueAxisLabel;
        this.binAxisFontSize = other.binAxisFontSize;
        this.valueAxisFontSize = other.valueAxisFontSize;
    }

    public static HistogramPlotData importFrom(Path storagePath) {
        return PlotData.importFrom(storagePath, HistogramPlotData.class);
    }

    @Override
    public JFreeChart getChart() {
        Set<String> existingSeriesNames = new HashSet<>();
        HistogramDataset dataset = new HistogramDataset();
        switch (histogramType) {
            case Frequency:
                dataset.setType(HistogramType.FREQUENCY);
                break;
            case RelativeFrequency:
                dataset.setType(HistogramType.RELATIVE_FREQUENCY);
                break;
            case ScaleAreaToOne:
                dataset.setType(HistogramType.SCALE_AREA_TO_1);
                break;
        }
        for (PlotDataSeries series : getSeries()) {
            String name = StringUtils.isNullOrEmpty(series.getName()) ? "Series" : series.getName();
            name = StringUtils.makeUniqueString(name, " ", existingSeriesNames);
            existingSeriesNames.add(name);

            dataset.addSeries(name, series.getColumnAsDouble("Value"), bins);
        }
        JFreeChart chart = ChartFactory.createHistogram(getTitle(), getBinAxisLabel(), getValueAxisLabel(), dataset);
        ((XYBarRenderer) chart.getXYPlot().getRenderer()).setBarPainter(new StandardXYBarPainter());
        chart.getXYPlot().setDomainGridlinePaint(getGridColor());
        chart.getXYPlot().getDomainAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, binAxisFontSize));
        chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, binAxisFontSize));
        chart.getXYPlot().getRangeAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, valueAxisFontSize));
        chart.getXYPlot().getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, valueAxisFontSize));
        updateChartProperties(chart);
        return chart;
    }

    @JIPipeDocumentation(name = "Bin axis label", description = "Label of the X-axis")
    @JIPipeParameter("bin-axis-label")
    public String getBinAxisLabel() {
        return binAxisLabel;
    }

    @JIPipeParameter("bin-axis-label")
    public void setBinAxisLabel(String binAxisLabel) {
        this.binAxisLabel = binAxisLabel;

    }

    @JIPipeDocumentation(name = "Value axis label", description = "Label of the Y-axis")
    @JIPipeParameter("value-axis-label")
    public String getValueAxisLabel() {
        return valueAxisLabel;
    }

    @JIPipeParameter("value-axis-label")
    public void setValueAxisLabel(String valueAxisLabel) {
        this.valueAxisLabel = valueAxisLabel;

    }

    @JIPipeDocumentation(name = "Bins", description = "Number of bins")
    @JIPipeParameter("bins")
    public int getBins() {
        return bins;
    }

    @JIPipeParameter("bins")
    public void setBins(int bins) {
        this.bins = bins;

    }

    @JIPipeDocumentation(name = "Histogram type", description = "Type of histogram to generate")
    @JIPipeParameter("histogram-type")
    public HistogramType_ getHistogramType() {
        return histogramType;
    }

    @JIPipeParameter("histogram-type")
    public void setHistogramType(HistogramType_ histogramType) {
        this.histogramType = histogramType;

    }

    @JIPipeDocumentation(name = "Bin axis font size", description = "Font size of the bin axis")
    @JIPipeParameter("bin-axis-font-size")
    public int getBinAxisFontSize() {
        return binAxisFontSize;
    }

    @JIPipeParameter("bin-axis-font-size")
    public void setBinAxisFontSize(int binAxisFontSize) {
        this.binAxisFontSize = binAxisFontSize;
    }

    @JIPipeDocumentation(name = "Value axis font size", description = "Font size of the value axis")
    @JIPipeParameter("value-axis-font-size")
    public int getValueAxisFontSize() {
        return valueAxisFontSize;
    }

    @JIPipeParameter("value-axis-font-size")
    public void setValueAxisFontSize(int valueAxisFontSize) {
        this.valueAxisFontSize = valueAxisFontSize;
    }

    /**
     * Available histogram types
     */
    public enum HistogramType_ {
        Frequency,
        RelativeFrequency,
        ScaleAreaToOne
    }
}
