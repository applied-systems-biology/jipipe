package org.hkijena.jipipe.extensions.forms.datatypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotData;
import org.hkijena.jipipe.extensions.plots.datatypes.PlotDataSeries;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for any form data.
 * A form data object is a {@link JIPipeParameterCollection} and is (de)serialized via the parameter system.
 * Its standard display function shows the user interface with placeholder values
 */
@JIPipeDocumentation(name = "Form", description = "Data that describes a user input element.")
@JsonSerialize(using = FormData.Serializer.class)
public abstract class FormData implements JIPipeData, JIPipeParameterCollection, JIPipeValidatable {

    private final EventBus eventBus = new EventBus();
    private TabSettings tabSettings = new TabSettings();

    public FormData() {
        tabSettings.getEventBus().register(this);
    }

    public FormData(FormData other) {
        this.tabSettings = new TabSettings(other.tabSettings);
        tabSettings.getEventBus().register(this);
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path fileName = forceName ? Paths.get(name) : Paths.get("form.json");
        try {
            JsonUtils.getObjectMapper().writeValue(storageFilePath.resolve(fileName).toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Component preview(int width, int height) {
        return null;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * This method is called by the form processor nodes on loading data into this form.
     * @param dataBatch the data batch
     */
    public abstract void loadData(JIPipeMergingDataBatch dataBatch);

    /**
     * This method should write any changes into the data batch
     * @param dataBatch the data batch
     */
    public abstract void writeData(JIPipeMergingDataBatch dataBatch);

    /**
     * Loads metadata from JSON
     *
     * @param node JSON node
     */
    public void fromJson(JsonNode node) {
        JIPipeParameterCollection.deserializeParametersFromJson(this, node, new JIPipeValidityReport());
    }

    @JIPipeDocumentation(name = "Form element tab", description = "Form elements can be displayed in different tabs for ease of use. " +
            "Change following settings to determine where this element is placed.")
    @JIPipeParameter("form:tabs")
    public TabSettings getTabSettings() {
        return tabSettings;
    }

    /**
     * Helper method that simplifies the importFrom() method definition
     * @param storageFilePath the storage folder
     * @param klass the form class
     * @param <T> the form class
     * @return the deserialized form
     */
    public static <T extends FormData> T importFrom(Path storageFilePath, Class<T> klass) {
        try {
            FormData formData = JsonUtils.getObjectMapper().readerFor(klass).readValue(storageFilePath.resolve("form.json").toFile());
            return (T) formData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Serializer extends JsonSerializer<FormData> {
        @Override
        public void serialize(FormData value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            JIPipeParameterCollection.serializeParametersToJson(value, gen);
            gen.writeEndObject();
        }
    }

    public static class TabSettings implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private String tab = "General";
        private OptionalAnnotationNameParameter tabAnnotation = new OptionalAnnotationNameParameter("Tab", true);

        public TabSettings() {
        }

        public TabSettings(TabSettings other) {
            this.tab = other.tab;
            this.tabAnnotation = new OptionalAnnotationNameParameter(other.tabAnnotation);
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Tab", description = "The tab where this form data will appear in.")
        @JIPipeParameter("form:tab")
        public String getTab() {
            return tab;
        }

        @JIPipeParameter("form:tab")
        public void setTab(String tab) {
            this.tab = tab;
        }

        @JIPipeDocumentation(name = "Tab annotation", description = "The annotation that contains the tab name.")
        @JIPipeParameter("form:tab-annotation")
        public OptionalAnnotationNameParameter getTabAnnotation() {
            return tabAnnotation;
        }

        @JIPipeParameter("form:tab-annotation")
        public void setTabAnnotation(OptionalAnnotationNameParameter tabAnnotation) {
            this.tabAnnotation = tabAnnotation;
        }
    }
}
