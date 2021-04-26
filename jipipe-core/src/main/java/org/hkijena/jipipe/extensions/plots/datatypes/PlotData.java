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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.plots.CacheAwarePlotEditor;
import org.hkijena.jipipe.extensions.plots.utils.ColorMap;
import org.hkijena.jipipe.extensions.plots.utils.ColorMapSupplier;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.plotbuilder.JIPipePlotBuilderUI;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains all necessary data to generate a plot
 */
@JIPipeDocumentation(name = "Plot", description = "A plot")
@JsonSerialize(using = PlotData.Serializer.class)
@JIPipeDataStorageDocumentation("The folder contains following files:<br/>" +
        "<ul>" +
        "<li><code>plot-metadata.json</code> contains the serialized information about the plot.</li>" +
        "<li><code>series[Index].csv</code> contains the data of series [Index].</li>" +
        "</ul><br/><br/>" +
        "The plot metadata JSON contains entries <code>title</code>, <code>export-width</code>," +
        "<code>export-height</code>, <code>background-color</code>, <code>grid-color</code>, " +
        "<code>with-legend</code>, <code>title-font-size</code>, <code>legend-font-size</code>, <code>color-map</code>, and <code>plot-series</code>.<br/>" +
        "<code>plot-series</code> is mandatory and is a list of objects with each object having an object <code>metadata</code>, and " +
        "a string element <code>file-name</code>. The file name must point at the corresponding <code>series[Index].csv</code> file of the series. " +
        "Additional metadata in the root object and series metadata depend on the exact plot type.")
public abstract class PlotData implements JIPipeData, JIPipeParameterCollection, JIPipeValidatable {

    private EventBus eventBus = new EventBus();
    private String title;
    private int exportWidth = 1024;
    private int exportHeight = 768;
    private Color backgroundColor = Color.WHITE;
    private Color gridColor = Color.GRAY;
    private boolean withLegend = true;
    private List<PlotDataSeries> series = new ArrayList<>();
    private int titleFontSize = 26;
    private int legendFontSize = 12;
    private ColorMap colorMap = ColorMap.Pastel1;

    /**
     * Creates a new empty instance
     */
    public PlotData() {

    }

    /**
     * Creates a copy of the provided data
     *
     * @param other the original
     */
    public PlotData(PlotData other) {
        this.title = other.title;
        this.exportWidth = other.exportWidth;
        this.exportHeight = other.exportHeight;
        this.backgroundColor = other.backgroundColor;
        this.gridColor = other.gridColor;
        this.withLegend = other.withLegend;
        this.titleFontSize = other.titleFontSize;
        this.legendFontSize = other.legendFontSize;
        this.colorMap = other.colorMap;
        for (PlotDataSeries data : other.series) {
            this.series.add(new PlotDataSeries(data));
        }
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        if (source instanceof JIPipeCacheSlotDataSource) {
            CacheAwarePlotEditor.show(workbench, (JIPipeCacheSlotDataSource) source, displayName);
        } else {
            JIPipePlotBuilderUI plotBuilderUI = new JIPipePlotBuilderUI(workbench);
            plotBuilderUI.importExistingPlot((PlotData) duplicate());
            workbench.getDocumentTabPane().addTab(displayName, UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                    plotBuilderUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
            workbench.getDocumentTabPane().switchToLastTab();
        }
    }

    @Override
    public Component preview(int width, int height) {
        BufferedImage image = getChart().createBufferedImage(exportWidth, exportHeight);
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.max(factorX, factorY);
        int imageWidth = (int) (image.getWidth() * factor);
        int imageHeight = (int) (image.getHeight() * factor);
        return new JLabel(new ImageIcon(image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH)));
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        // Export metadata
        try {
            if (!forceName) {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(storageFilePath.resolve("plot-metadata.json").toFile(), this);
            } else {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(storageFilePath.resolve(name + "_plot-metadata.json").toFile(), this);
            }
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to export plot!",
                    "Internal plot-export function",
                    "A plot should be saved to '" + storageFilePath + "'. There was an error during this export.",
                    "Please check if you can write to the output folder. Please check if the algorithm inputs are valid. " +
                            "If you cannot solve the issue, please contact the plugin author.");
        }

        // Export series
        for (int i = 0; i < series.size(); ++i) {
            if (forceName) {
                series.get(i).saveTo(storageFilePath, name + "_" + "series" + i, forceName, progressInfo);
            } else {
                series.get(i).saveTo(storageFilePath, "series" + i, forceName, progressInfo);
            }
        }

        // Export plot
        try {
            JFreeChart chart = getChart();

            // Save as PNG
            ChartUtils.saveChartAsPNG(storageFilePath.resolve(name + ".png").toFile(),
                    chart,
                    exportWidth,
                    exportHeight);

            // Save as SVG
            SVGGraphics2D g2 = new SVGGraphics2D(exportWidth, exportHeight);
            Rectangle r = new Rectangle(0, 0, exportWidth, exportHeight);
            chart.draw(g2, r);
            SVGUtils.writeToSVG(storageFilePath.resolve(name + ".svg").toFile(), g2.getSVGElement());

        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to export plot!",
                    "Internal plot-export function",
                    "A plot should be saved to '" + storageFilePath + "'. There was an error during this export.",
                    "Please check if you can write to the output folder. Please check if the algorithm inputs are valid. " +
                            "If you cannot solve the issue, please contact the plugin author.");
        }
    }

    @Override
    public JIPipeData duplicate() {
        return JIPipe.createData(getClass(), this);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public abstract JFreeChart getChart();

    /**
     * @return The plot title
     */
    @JIPipeDocumentation(name = "Title", description = "The title of this plot.")
    @JIPipeParameter("title")
    public String getTitle() {
        return title;
    }

    /**
     * Sets the plot title
     *
     * @param title The title
     */
    @JIPipeParameter("title")
    public void setTitle(String title) {
        this.title = title;
        eventBus.post(new ParameterChangedEvent(this, "title"));
    }

    @JIPipeDocumentation(name = "Export width", description = "Width of the output image generated via an export")
    @JIPipeParameter("export-width")
    public int getExportWidth() {
        return exportWidth;
    }

    @JIPipeParameter("export-width")
    public void setExportWidth(int exportWidth) {
        this.exportWidth = exportWidth;
        eventBus.post(new ParameterChangedEvent(this, "export-width"));
    }

    @JIPipeDocumentation(name = "Export height", description = "Height of the output image generated via an export")
    @JIPipeParameter("export-height")
    public int getExportHeight() {
        return exportHeight;
    }

    @JIPipeParameter("export-height")
    public void setExportHeight(int exportHeight) {
        this.exportHeight = exportHeight;
        eventBus.post(new ParameterChangedEvent(this, "export-height"));
    }

    @JIPipeDocumentation(name = "Background color", description = "Background color of the plot area.")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @JIPipeDocumentation(name = "Grid color", description = "Color of the grid")
    @JIPipeParameter("grid-color")
    public Color getGridColor() {
        return gridColor;
    }

    @JIPipeParameter("grid-color")
    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
    }

    @JIPipeDocumentation(name = "Show legend", description = "If enabled, a legend is shown.")
    @JIPipeParameter("with-legend")
    public boolean isWithLegend() {
        return withLegend;
    }

    @JIPipeParameter("with-legend")
    public void setWithLegend(boolean withLegend) {
        this.withLegend = withLegend;
    }

    @JIPipeDocumentation(name = "Title font size", description = "Font size of the title")
    @JIPipeParameter("title-font-size")
    public int getTitleFontSize() {
        return titleFontSize;
    }

    @JIPipeParameter("title-font-size")
    public boolean setTitleFontSize(int titleFontSize) {
        if (titleFontSize <= 0)
            return false;
        this.titleFontSize = titleFontSize;
        return true;
    }

    @JIPipeDocumentation(name = "Legend font size", description = "The font size of legends")
    @JIPipeParameter("legend-font-size")
    public int getLegendFontSize() {
        return legendFontSize;
    }

    @JIPipeParameter("legend-font-size")
    public void setLegendFontSize(int legendFontSize) {
        this.legendFontSize = legendFontSize;
    }

    @JIPipeDocumentation(name = "Color map", description = "Determines how elements are colored")
    @JIPipeParameter("color-map")
    public ColorMap getColorMap() {
        return colorMap;
    }

    @JIPipeParameter("color-map")
    public void setColorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
    }

    /**
     * Sets properties of charts
     *
     * @param chart the chart
     */
    protected void updateChartProperties(JFreeChart chart) {
        chart.getPlot().setBackgroundPaint(getBackgroundColor());
        if (chart.getLegend() != null) {
            if (!isWithLegend())
                chart.removeLegend();
            else {
                chart.getLegend().setItemFont(new Font(Font.SANS_SERIF, Font.PLAIN, getLegendFontSize()));
            }
        }
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, getTitleFontSize()));
        }
        chart.getPlot().setDrawingSupplier(new ColorMapSupplier(colorMap.getColors()));
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Export width").checkIfWithin(this, exportWidth, 0, Double.POSITIVE_INFINITY, false, true);
        report.forCategory("Export height").checkIfWithin(this, exportHeight, 0, Double.POSITIVE_INFINITY, false, true);
    }

    public List<PlotDataSeries> getSeries() {
        return Collections.unmodifiableList(series);
    }

    /**
     * Adds a new series into this plot
     *
     * @param series the series
     */
    public void addSeries(PlotDataSeries series) {
        this.series.add(series);

    }

    /**
     * Removes a series
     *
     * @param series the series
     */
    public void removeSeries(PlotDataSeries series) {
        this.series.remove(series);

    }

    /**
     * Removes all series
     */
    public void clearSeries() {
        this.series.clear();

    }

    @Override
    public String toString() {
        return JIPipeDataInfo.getInstance(getClass()).getName();
    }

    /**
     * Loads metadata from JSON
     *
     * @param node JSON node
     */
    public void fromJson(JsonNode node) {
        JIPipeParameterCollection.deserializeParametersFromJson(this, node, new JIPipeValidityReport());
    }

    public static <T extends PlotData> T importFrom(Path storageFilePath, Class<T> klass) {
        try {
            JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(storageFilePath.resolve("plot-metadata.json").toFile());
            PlotData plotData = JsonUtils.getObjectMapper().readerFor(klass).readValue(node);
            JIPipeParameterCollection.deserializeParametersFromJson(plotData, node, new JIPipeValidityReport());
            List<Path> seriesFiles = PathUtils.findFilesByExtensionIn(storageFilePath, ".csv").stream()
                    .filter(p -> p.getFileName().toString().matches("series\\d+.csv")).sorted(Comparator.comparing(p -> p.getFileName().toString())).collect(Collectors.toList());
            for (Path seriesFile : seriesFiles) {
                plotData.addSeries(new PlotDataSeries(ResultsTable.open(seriesFile.toString())));
            }
            return (T) plotData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads data from a folder
     *
     * @param folder folder
     * @return loaded data
     */
    public static PlotData fromFolder(Path folder) {
        PlotData result;
        try {
            JsonNode node = JsonUtils.getObjectMapper().readValue(folder.resolve("plot-metadata.json").toFile(), JsonNode.class);
            Class<? extends JIPipeData> dataClass = JIPipe.getDataTypes().getById(node.get("plot-data-type").textValue());
            result = (PlotData) JIPipe.createData(dataClass);

            // Load metadata
            result.fromJson(node);

            // Load series
            for (JsonNode element : ImmutableList.copyOf(node.get("plot-series").elements())) {
                PlotDataSeries series = JsonUtils.getObjectMapper().readerFor(PlotDataSeries.class).readValue(element.get("metadata"));
                Path fileName = folder.resolve(element.get("file-name").textValue());
                ResultsTableData tableData = new ResultsTableData(ResultsTable.open(fileName.toString()));
                series.setTable(tableData.getTable());
                result.addSeries(series);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Serializes the metadata of {@link PlotData}
     */
    public static class Serializer extends JsonSerializer<PlotData> {
        @Override
        public void serialize(PlotData value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            gen.writeStringField("plot-data-type", JIPipe.getDataTypes().getIdOf(value.getClass()));

            // Write parameters
            JIPipeParameterCollection.serializeParametersToJson(value, gen);

            // Write series mapping
            gen.writeFieldName("plot-series");
            gen.writeStartArray();
            for (int i = 0; i < value.series.size(); ++i) {
                gen.writeStartObject();
                gen.writeStringField("file-name", "series" + i + ".csv");
                gen.writeObjectField("metadata", value.series.get(i));
                gen.writeEndObject();
            }
            gen.writeEndArray();

            gen.writeEndObject();
        }
    }
}
