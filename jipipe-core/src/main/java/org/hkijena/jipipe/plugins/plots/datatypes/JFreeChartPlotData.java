/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.plots.datatypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeCommonData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.colors.ColorListParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.plots.utils.ColorMap;
import org.hkijena.jipipe.plugins.plots.utils.ColorMapSupplier;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

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
@SetJIPipeDocumentation(name = "JFreeChart Plot", description = "A plot")
@JsonSerialize(using = JFreeChartPlotData.Serializer.class)
@JIPipeDataStorageDocumentation(humanReadableDescription = "The folder contains following files:<br/>" +
        "<ul>" +
        "<li><code>plot-metadata.json</code> contains the serialized information about the plot.</li>" +
        "<li><code>series[Index].csv</code> contains the data of series [Index].</li>" +
        "</ul><br/><br/>" +
        "The plot metadata JSON contains entries <code>title</code>, <code>export-width</code>," +
        "<code>export-height</code>, <code>background-color</code>, <code>grid-color</code>, " +
        "<code>with-legend</code>, <code>title-font-size</code>, <code>legend-font-size</code>, <code>color-map</code>, and <code>plot-series</code>.<br/>" +
        "<code>plot-series</code> is mandatory and is a list of objects with each object having an object <code>metadata</code>, and " +
        "a string element <code>file-name</code>. The file name must point at the corresponding <code>series[Index].csv</code> file of the series. " +
        "Additional metadata in the root object and series metadata depend on the exact plot type.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/plot-data.schema.json")
@LabelAsJIPipeCommonData
public abstract class JFreeChartPlotData extends AbstractJIPipeParameterCollection implements JIPipeData, JIPipeValidatable {
    private final List<JFreeChartPlotDataSeries> series = new ArrayList<>();
    private String title;
    private int exportWidth = 1024;
    private int exportHeight = 768;
    private Color backgroundColor = Color.WHITE;
    private Color gridColor = Color.GRAY;
    private boolean withLegend = true;
    private int titleFontSize = 26;
    private int legendFontSize = 12;
    private ColorMap colorMap = ColorMap.Pastel1;
    private boolean useCustomColorMap = false;
    private ColorListParameter customColorMap = new ColorListParameter();

    /**
     * Creates a new empty instance
     */
    public JFreeChartPlotData() {

    }

    /**
     * Creates a copy of the provided data
     *
     * @param other the original
     */
    public JFreeChartPlotData(JFreeChartPlotData other) {
        this.title = other.title;
        this.exportWidth = other.exportWidth;
        this.exportHeight = other.exportHeight;
        this.backgroundColor = other.backgroundColor;
        this.gridColor = other.gridColor;
        this.withLegend = other.withLegend;
        this.titleFontSize = other.titleFontSize;
        this.legendFontSize = other.legendFontSize;
        this.colorMap = other.colorMap;
        for (JFreeChartPlotDataSeries data : other.series) {
            this.series.add(new JFreeChartPlotDataSeries(data));
        }
        this.useCustomColorMap = other.useCustomColorMap;
        this.customColorMap = new ColorListParameter(other.customColorMap);
    }

    public static <T extends JFreeChartPlotData> T importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        try {
            Path storageFilePath = storage.getFileSystemPath();
            JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(storageFilePath.resolve("plot-metadata.json").toFile());
            String dataTypeId = node.get("plot-data-type").textValue();
            Class<? extends JIPipeData> klass = JIPipe.getDataTypes().getById(dataTypeId);
            JFreeChartPlotData plotData = JsonUtils.getObjectMapper().readerFor(klass).readValue(node);
            ParameterUtils.deserializeParametersFromJson(plotData, node, new UnspecifiedValidationReportContext(), new JIPipeValidationReport());
            List<Path> seriesFiles = PathUtils.findFilesByExtensionIn(storageFilePath, ".csv").stream()
                    .filter(p -> p.getFileName().toString().matches("series\\d+.csv")).sorted(Comparator.comparing(p -> p.getFileName().toString())).collect(Collectors.toList());
            for (Path seriesFile : seriesFiles) {
                plotData.addSeries(new JFreeChartPlotDataSeries(ResultsTable.open(seriesFile.toString())));
            }
            return (T) plotData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends JFreeChartPlotData> T importData(JIPipeReadDataStorage storage, Class<T> klass, JIPipeProgressInfo progressInfo) {
        try {
            Path storageFilePath = storage.getFileSystemPath();
            JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(storageFilePath.resolve("plot-metadata.json").toFile());
            JFreeChartPlotData plotData = JsonUtils.getObjectMapper().readerFor(klass).readValue(node);
            ParameterUtils.deserializeParametersFromJson(plotData, node, new UnspecifiedValidationReportContext(), new JIPipeValidationReport());
            List<Path> seriesFiles = PathUtils.findFilesByExtensionIn(storageFilePath, ".csv").stream()
                    .filter(p -> p.getFileName().toString().matches("series\\d+.csv")).sorted(Comparator.comparing(p -> p.getFileName().toString())).collect(Collectors.toList());
            for (Path seriesFile : seriesFiles) {
                plotData.addSeries(new JFreeChartPlotDataSeries(ResultsTable.open(seriesFile.toString())));
            }
            return (T) plotData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calibrates the axes of a plot, setting min and max values
     *
     * @param axis the axis
     * @param min  the min value
     * @param max  the max value
     */
    public static void calibrateAxis(ValueAxis axis, OptionalDoubleParameter min, OptionalDoubleParameter max) {
        double _min = Double.NEGATIVE_INFINITY;
        double _max = Double.POSITIVE_INFINITY;
        if (min.isEnabled()) {
            _min = min.getContent();
        }
        if (max.isEnabled()) {
            _max = max.getContent();
        }
        if (!Double.isFinite(_min) && !Double.isFinite(_max)) {
            axis.setAutoRange(true);
            return;
        }
        if (!Double.isFinite(_min) || !Double.isFinite(_max)) {
            axis.setAutoRange(true);
            if (!Double.isFinite(_min)) {
                _min = axis.getLowerBound();
            }
            if (!Double.isFinite(_max)) {
                _max = axis.getUpperBound();
            }
        }
        axis.setRange(_min, _max);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        BufferedImage image = getChart().createBufferedImage(exportWidth, exportHeight);
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.max(factorX, factorY);
        int imageWidth = (int) (image.getWidth() * factor);
        int imageHeight = (int) (image.getHeight() * factor);
        return new JIPipeImageThumbnailData(image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH));
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        // Export metadata
        try {
            if (!forceName) {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(storage.getFileSystemPath().resolve("plot-metadata.json").toFile(), this);
            } else {
                JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(storage.getFileSystemPath().resolve(name + "_plot-metadata.json").toFile(), this);
            }
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e, "Unable to export plot!",
                    "A plot should be saved to '" + storage + "'. There was an error during this export.",
                    "Please check if you can write to the output folder. Please check if the algorithm inputs are valid. " +
                            "If you cannot solve the issue, please contact the plugin author.");
        }

        // Export series
        for (int i = 0; i < series.size(); ++i) {
            if (forceName) {
                series.get(i).exportData(storage, name + "_" + "series" + i, forceName, progressInfo);
            } else {
                series.get(i).exportData(storage, "series" + i, forceName, progressInfo);
            }
        }

        // Export plot
        try {
            JFreeChart chart = getChart();

            // Save as PNG
            ChartUtils.saveChartAsPNG(storage.getFileSystemPath().resolve(name + ".png").toFile(),
                    chart,
                    exportWidth,
                    exportHeight);

            // Save as SVG
            SVGGraphics2D g2 = new SVGGraphics2D(exportWidth, exportHeight);
            Rectangle r = new Rectangle(0, 0, exportWidth, exportHeight);
            chart.draw(g2, r);
            SVGUtils.writeToSVG(storage.getFileSystemPath().resolve(name + ".svg").toFile(), g2.getSVGElement());

        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e, "Unable to export plot!",
                    "A plot should be saved to '" + storage + "'. There was an error during this export.",
                    "Please check if you can write to the output folder. Please check if the algorithm inputs are valid. " +
                            "If you cannot solve the issue, please contact the plugin author.");
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return JIPipe.createData(getClass(), this);
    }

    public abstract JFreeChart getChart();

    /**
     * @return The plot title
     */
    @SetJIPipeDocumentation(name = "Title", description = "The title of this plot.")
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
    }

    @SetJIPipeDocumentation(name = "Export width", description = "Width of the output image generated via an export")
    @JIPipeParameter("export-width")
    public int getExportWidth() {
        return exportWidth;
    }

    @JIPipeParameter("export-width")
    public void setExportWidth(int exportWidth) {
        this.exportWidth = exportWidth;
    }

    @SetJIPipeDocumentation(name = "Export height", description = "Height of the output image generated via an export")
    @JIPipeParameter("export-height")
    public int getExportHeight() {
        return exportHeight;
    }

    @JIPipeParameter("export-height")
    public void setExportHeight(int exportHeight) {
        this.exportHeight = exportHeight;
    }

    @SetJIPipeDocumentation(name = "Background color", description = "Background color of the plot area.")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @SetJIPipeDocumentation(name = "Grid color", description = "Color of the grid")
    @JIPipeParameter("grid-color")
    public Color getGridColor() {
        return gridColor;
    }

    @JIPipeParameter("grid-color")
    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
    }

    @SetJIPipeDocumentation(name = "Show legend", description = "If enabled, a legend is shown.")
    @JIPipeParameter("with-legend")
    public boolean isWithLegend() {
        return withLegend;
    }

    @JIPipeParameter("with-legend")
    public void setWithLegend(boolean withLegend) {
        this.withLegend = withLegend;
    }

    @SetJIPipeDocumentation(name = "Title font size", description = "Font size of the title")
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

    @SetJIPipeDocumentation(name = "Legend font size", description = "The font size of legends")
    @JIPipeParameter("legend-font-size")
    public int getLegendFontSize() {
        return legendFontSize;
    }

    @JIPipeParameter("legend-font-size")
    public void setLegendFontSize(int legendFontSize) {
        this.legendFontSize = legendFontSize;
    }

    @SetJIPipeDocumentation(name = "Color map", description = "Determines how elements are colored")
    @JIPipeParameter("color-map")
    public ColorMap getColorMap() {
        return colorMap;
    }

    @JIPipeParameter("color-map")
    public void setColorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
    }

    @SetJIPipeDocumentation(name = "Use custom color map", description = "If enabled, you can define a custom color map")
    @JIPipeParameter("use-custom-color-map")
    public boolean isUseCustomColorMap() {
        return useCustomColorMap;
    }

    @JIPipeParameter("use-custom-color-map")
    public void setUseCustomColorMap(boolean useCustomColorMap) {
        this.useCustomColorMap = useCustomColorMap;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Custom color map", description = "Add colors into this list to define a custom color map")
    @JIPipeParameter("custom-color-map")
    public ColorListParameter getCustomColorMap() {
        return customColorMap;
    }

    @JIPipeParameter("custom-color-map")
    public void setCustomColorMap(ColorListParameter customColorMap) {
        this.customColorMap = customColorMap;
    }

    public Paint[] getCurrentColorMap() {
        if (useCustomColorMap && !customColorMap.isEmpty()) {
            Paint[] result = new Paint[customColorMap.size()];
            for (int i = 0; i < customColorMap.size(); i++) {
                Color color = customColorMap.get(i);
                result[i] = color;
            }
            return result;
        } else {
            return colorMap.getColors();
        }
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (!useCustomColorMap && "custom-color-map".equals(access.getKey()) && access.getSource() == this) {
            return false;
        }
        if (useCustomColorMap && "color-map".equals(access.getKey()) && access.getSource() == this) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
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
        chart.getPlot().setDrawingSupplier(new ColorMapSupplier(getCurrentColorMap()));
    }

    /**
     * Gets the metadata for this plot instance.
     *
     * @return the metadata
     */
    public JFreeChartPlotMetadata getMetadata() {
        return getClass().getAnnotation(JFreeChartPlotMetadata.class);
    }

    /**
     * Generates an empty series table with the correct columns
     *
     * @return the series table
     */
    public ResultsTableData createSeriesTable() {
        ResultsTableData result = new ResultsTableData();
        for (JFreeChartPlotColumn column : getMetadata().columns()) {
            result.addColumn(column.name(), !column.isNumeric());
        }
        return result;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (exportWidth <= 0) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, reportContext, "Export width is too small!", "The export width must be at least 1"));
        }
        if (exportHeight <= 0) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, reportContext, "Export height is too small!", "The export height must be at least 1"));
        }
    }

    public List<JFreeChartPlotDataSeries> getSeries() {
        return Collections.unmodifiableList(series);
    }

    /**
     * Adds a new series into this plot
     *
     * @param series the series
     */
    public void addSeries(JFreeChartPlotDataSeries series) {
        this.series.add(series);

    }

    /**
     * Removes a series
     *
     * @param series the series
     */
    public void removeSeries(JFreeChartPlotDataSeries series) {
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
        ParameterUtils.deserializeParametersFromJson(this, node, new UnspecifiedValidationReportContext(), new JIPipeValidationReport());
    }

    /**
     * Serializes the metadata of {@link JFreeChartPlotData}
     */
    public static class Serializer extends JsonSerializer<JFreeChartPlotData> {
        @Override
        public void serialize(JFreeChartPlotData value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            gen.writeStringField("plot-data-type", JIPipe.getDataTypes().getIdOf(value.getClass()));

            // Write parameters
            ParameterUtils.serializeParametersToJson(value, gen);

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
