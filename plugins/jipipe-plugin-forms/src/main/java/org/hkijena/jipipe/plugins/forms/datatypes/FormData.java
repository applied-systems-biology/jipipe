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

package org.hkijena.jipipe.plugins.forms.datatypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for any form data.
 * A form data object is a {@link JIPipeParameterCollection} and is (de)serialized via the parameter system.
 * Its standard display function shows the user interface with placeholder values
 */
@SetJIPipeDocumentation(name = "Form", description = "Data that describes a user input element.")
@JsonSerialize(using = FormData.Serializer.class)
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a file forms.json that stores all metadata of the current form type in JSON format.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/form-data.schema.json")
public abstract class FormData extends AbstractJIPipeParameterCollection implements JIPipeData, JIPipeValidatable {

    private TabSettings tabSettings = new TabSettings();

    public FormData() {
    }

    public FormData(FormData other) {
        this.tabSettings = new TabSettings(other.tabSettings);
    }

    /**
     * Helper method that simplifies the importData() method definition
     *
     * @param <T>          the form class
     * @param storage      the storage folder
     * @param klass        the form class
     * @param progressInfo the progress info
     * @return the deserialized form
     */
    public static <T extends FormData> T importData(JIPipeReadDataStorage storage, Class<T> klass, JIPipeProgressInfo progressInfo) {
        try {
            FormData formData = JsonUtils.getObjectMapper().readerFor(klass).readValue(storage.getFileSystemPath().resolve("form.json").toFile());
            return (T) formData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path fileName = forceName ? Paths.get(name) : Paths.get("form.json");
        try {
            JsonUtils.getObjectMapper().writeValue(storage.getFileSystemPath().resolve(fileName).toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a component that acts as the editor for the form data.
     * This is presented to the user
     *
     * @param workbench the workbench
     * @return the editor
     */
    public abstract Component getEditor(JIPipeDesktopWorkbench workbench);

    /**
     * This method is called by the form processor nodes on loading data into this form.
     *
     * @param iterationStep the iteration step
     */
    public abstract void loadData(JIPipeMultiIterationStep iterationStep);

    /**
     * This method should write any changes into the iteration step
     *
     * @param iterationStep the iteration step
     */
    public abstract void writeData(JIPipeMultiIterationStep iterationStep);

    /**
     * Loads metadata from JSON
     *
     * @param node JSON node
     */
    public void fromJson(JsonNode node) {
        ParameterUtils.deserializeParametersFromJson(this, node, new UnspecifiedValidationReportContext(), new JIPipeValidationReport());
    }

    /**
     * Return true if this form data is replaceable via the "Apply to ..." function
     *
     * @return if the form data is immutable
     */
    public boolean isImmutable() {
        return false;
    }

    /**
     * Determines whether this form data uses a custom copy method for the "Apply to ..." function
     * If false, duplicate() is used
     * If true, customCopy(src) is used
     *
     * @return if custom copy is enabled
     */
    public boolean isUsingCustomCopy() {
        return false;
    }

    /**
     * Determines whether this form data uses a custom reset method
     * If false, a new copy of the original data is created
     * If true, customReset() is called
     *
     * @return if custom reset is enabled
     */
    public boolean isUsingCustomReset() {
        return false;
    }

    /**
     * A custom copy function that copies the contents from source into this form.
     * Should never raise exceptions. Use {@link JIPipeValidationReport} to report issues.
     *
     * @param source  the source data
     * @param context the context
     * @param report  the error report
     */
    public void customCopy(FormData source, CustomValidationReportContext context, JIPipeValidationReport report) {

    }

    /**
     * A custom reset method
     */
    public void customReset() {

    }

    @SetJIPipeDocumentation(name = "Form element tab", description = "Form elements can be displayed in different tabs for ease of use. " +
            "Change following settings to determine where this element is placed.")
    @JIPipeParameter("form:tabs")
    public TabSettings getTabSettings() {
        return tabSettings;
    }

    public static class Serializer extends JsonSerializer<FormData> {
        @Override
        public void serialize(FormData value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            ParameterUtils.serializeParametersToJson(value, gen);
            gen.writeEndObject();
        }
    }

    public static class TabSettings extends AbstractJIPipeParameterCollection {
        private String tab = "General";
        private OptionalTextAnnotationNameParameter tabAnnotation = new OptionalTextAnnotationNameParameter("Tab", true);

        public TabSettings() {
        }

        public TabSettings(TabSettings other) {
            this.tab = other.tab;
            this.tabAnnotation = new OptionalTextAnnotationNameParameter(other.tabAnnotation);
        }

        @SetJIPipeDocumentation(name = "Tab", description = "The tab where this form data will appear in.")
        @JIPipeParameter("form:tab")
        public String getTab() {
            return tab;
        }

        @JIPipeParameter("form:tab")
        public void setTab(String tab) {
            this.tab = tab;
        }

        @SetJIPipeDocumentation(name = "Tab annotation", description = "The annotation that contains the tab name.")
        @JIPipeParameter("form:tab-annotation")
        public OptionalTextAnnotationNameParameter getTabAnnotation() {
            return tabAnnotation;
        }

        @JIPipeParameter("form:tab-annotation")
        public void setTabAnnotation(OptionalTextAnnotationNameParameter tabAnnotation) {
            this.tabAnnotation = tabAnnotation;
        }
    }
}
