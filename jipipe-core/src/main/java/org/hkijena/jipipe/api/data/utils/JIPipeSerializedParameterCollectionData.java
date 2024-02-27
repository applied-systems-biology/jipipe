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

package org.hkijena.jipipe.api.data.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A base class for {@link JIPipeData} that is serialized from/to JSON for convenience
 * Only valid {@link org.hkijena.jipipe.api.parameters.JIPipeParameter} definitions are stored.
 * Ensure that this data type has a copy constructor for the duplicate() function.
 * You also still need to add the proper {@link SetJIPipeDocumentation} annotation and
 * the JIPipeData importData(Path) static function.
 * Unlike {@link JIPipeSerializedJsonObjectData}, this data is serialized in a way to fully restore its data type during deserialization,
 * removing the requirement of storing the data type ID within the JSON.
 */
@JIPipeDataStorageDocumentation(humanReadableDescription = "A JSON file that contains the serialized data",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
@JsonSerialize(using = JIPipeSerializedParameterCollectionData.Serializer.class)
@JsonDeserialize(using = JIPipeSerializedParameterCollectionData.Deserializer.class)
public abstract class JIPipeSerializedParameterCollectionData extends AbstractJIPipeParameterCollection implements JIPipeData {

    public JIPipeSerializedParameterCollectionData() {

    }

    public JIPipeSerializedParameterCollectionData(JIPipeSerializedParameterCollectionData other) {

    }

    public static JIPipeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        return JsonUtils.readFromFile(targetFile, JIPipeSerializedParameterCollectionData.class);
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        JsonUtils.saveToFile(this, storage.getFileSystemPath().resolve(StringUtils.orElse(name, "data") + ".json"));
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        JIPipeData copy = duplicate(new JIPipeProgressInfo());
        ParameterPanel.showDialog(workbench, (JIPipeParameterCollection) copy, new MarkdownDocument(""), displayName,
                ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return (JIPipeData) ReflectionUtils.newInstance(getClass(), this);
    }

    public static class Serializer extends JsonSerializer<JIPipeSerializedParameterCollectionData> {
        @Override
        public void serialize(JIPipeSerializedParameterCollectionData data, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("jipipe:data-type-id", JIPipe.getDataTypes().getIdOf(data.getClass()));
            jsonGenerator.writeFieldName("parameters");
            jsonGenerator.writeStartObject();
            ParameterUtils.serializeParametersToJson(data, jsonGenerator);
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<JIPipeSerializedParameterCollectionData> {
        @Override
        public JIPipeSerializedParameterCollectionData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            String dataTypeId = node.get("jipipe:data-type-id").textValue();
            Class<? extends JIPipeData> dataClass = JIPipe.getDataTypes().getById(dataTypeId);
            JIPipeSerializedParameterCollectionData data = (JIPipeSerializedParameterCollectionData) ReflectionUtils.newInstance(dataClass);
            JsonNode parameterData = node.get("parameters");
            ParameterUtils.deserializeParametersFromJson(data, parameterData, new UnspecifiedValidationReportContext(), new JIPipeValidationReport());
            return data;
        }
    }
}
