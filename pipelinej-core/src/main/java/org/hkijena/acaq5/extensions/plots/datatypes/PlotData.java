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

package org.hkijena.acaq5.extensions.plots.datatypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.tables.ResultsTableData;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.plotbuilder.ACAQPlotBuilderUI;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Contains all necessary data to generate a plot
 */
@ACAQDocumentation(name = "Plot", description = "A plot")
@JsonSerialize(using = PlotData.Serializer.class)
@ACAQHidden
public abstract class PlotData implements ACAQData, ACAQParameterCollection, ACAQValidatable {

    private EventBus eventBus = new EventBus();
    private String title;
    private int exportWidth = 1024;
    private int exportHeight = 768;
    private List<PlotDataSeries> series = new ArrayList<>();

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
        for (PlotDataSeries data : other.series) {
            this.series.add(new PlotDataSeries(data));
        }
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {
        ACAQPlotBuilderUI plotBuilderUI = new ACAQPlotBuilderUI(workbench);
        plotBuilderUI.importExistingPlot((PlotData) duplicate());
        workbench.getDocumentTabPane().addTab(displayName, UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        workbench.getDocumentTabPane().switchToLastTab();
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {
        // Export metadata
        try {
            if (forceName) {
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
                series.get(i).saveTo(storageFilePath, name + "_" + "series" + i, forceName);
            } else {
                series.get(i).saveTo(storageFilePath, "series" + i, forceName);
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
    public ACAQData duplicate() {
        return ACAQData.createInstance(getClass(), this);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public abstract JFreeChart getChart();

    /**
     * @return The plot title
     */
    @ACAQDocumentation(name = "Title", description = "The title of this plot.")
    @ACAQParameter("title")
    public String getTitle() {
        return title;
    }

    /**
     * Sets the plot title
     *
     * @param title The title
     */
    @ACAQParameter("title")
    public void setTitle(String title) {
        this.title = title;
        eventBus.post(new ParameterChangedEvent(this, "title"));
    }

    @ACAQDocumentation(name = "Export width", description = "Width of the output image generated via an export")
    @ACAQParameter("export-width")
    public int getExportWidth() {
        return exportWidth;
    }

    @ACAQParameter("export-width")
    public void setExportWidth(int exportWidth) {
        this.exportWidth = exportWidth;
        eventBus.post(new ParameterChangedEvent(this, "export-width"));
    }

    @ACAQDocumentation(name = "Export height", description = "Height of the output image generated via an export")
    @ACAQParameter("export-height")
    public int getExportHeight() {
        return exportHeight;
    }

    @ACAQParameter("export-height")
    public void setExportHeight(int exportHeight) {
        this.exportHeight = exportHeight;
        eventBus.post(new ParameterChangedEvent(this, "export-height"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
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
        return ACAQDataDeclaration.getInstance(getClass()).getName();
    }

    /**
     * Loads metadata from JSON
     *
     * @param node JSON node
     */
    public void fromJson(JsonNode node) {
        // Deserialize dynamic parameters
        if (node.has("acaq:dynamic-parameters")) {
            ACAQParameterTree parameterCollection = new ACAQParameterTree(this);
            Set<ACAQParameterCollection> dynamicParameters = parameterCollection.getRegisteredSources().stream()
                    .filter(src -> src instanceof ACAQDynamicParameterCollection).collect(Collectors.toSet());
            for (ACAQParameterCollection dynamicParameter : dynamicParameters) {
                String key = parameterCollection.getSourceKey(dynamicParameter);
                JsonNode entryNode = node.path("acaq:dynamic-parameters").path(key);
                if (!entryNode.isMissingNode()) {
                    ((ACAQDynamicParameterCollection) dynamicParameter).fromJson(entryNode);
                }
            }

        }

        // Deserialize algorithm-specific parameters
        AtomicBoolean changedStructure = new AtomicBoolean();
        changedStructure.set(true);
        getEventBus().register(new Object() {
            @Subscribe
            public void onParametersChanged(ParameterStructureChangedEvent event) {
                changedStructure.set(true);
            }
        });
        Set<String> loadedParameters = new HashSet<>();
        while (changedStructure.get()) {
            changedStructure.set(false);
            ACAQParameterTree parameterCollection = new ACAQParameterTree(this);
            for (ACAQParameterAccess parameterAccess : parameterCollection.getParametersByPriority()) {
                String key = parameterCollection.getUniqueKey(parameterAccess);
                if (loadedParameters.contains(key))
                    continue;
                loadedParameters.add(key);
                if (node.has(key)) {
                    Object v;
                    try {
                        v = JsonUtils.getObjectMapper().readerFor(parameterAccess.getFieldClass()).readValue(node.get(key));
                    } catch (IOException e) {
                        throw new UserFriendlyRuntimeException(e, "Could not load parameter '" + key + "'!",
                                "Plot data", "Either the data was corrupted, or your ACAQ5 or plugin version is too new or too old.",
                                "Check the 'dependencies' section of the project file and compare the plugin versions. Try " +
                                        "to update ACAQ5. Compare the project file with a valid one. Contact the ACAQ5 or plugin " +
                                        "authors if you cannot resolve the issue by yourself.");
                    }
                    parameterAccess.set(v);

                    // Stop loading here to prevent already traversed parameters from being not loaded
                    if (changedStructure.get())
                        break;
                }
            }
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
            Class<? extends ACAQData> dataClass = ACAQDatatypeRegistry.getInstance().getById(node.get("plot-data-type").textValue());
            result = (PlotData) ACAQData.createInstance(dataClass);

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
            gen.writeStringField("plot-data-type", ACAQDatatypeRegistry.getInstance().getIdOf(value.getClass()));

            // Write parameters
            ACAQParameterTree parameterCollection = new ACAQParameterTree(value);
            for (Map.Entry<String, ACAQParameterAccess> kv : parameterCollection.getParameters().entrySet()) {
                gen.writeObjectField(kv.getKey(), kv.getValue().get(Object.class));
            }

            // Save dynamic parameter storage
            Set<ACAQParameterCollection> dynamicParameters = parameterCollection.getRegisteredSources().stream()
                    .filter(src -> src instanceof ACAQDynamicParameterCollection).collect(Collectors.toSet());
            if (!dynamicParameters.isEmpty()) {
                gen.writeFieldName("acaq:dynamic-parameters");
                gen.writeStartObject();
                for (ACAQParameterCollection dynamicParameter : dynamicParameters) {
                    String key = parameterCollection.getSourceKey(dynamicParameter);
                    gen.writeObjectField(key, dynamicParameter);
                }
                gen.writeEndObject();
            }

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
