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

package org.hkijena.jipipe.extensions.multiparameters.datatypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the parameters of an algorithm
 */
@SetJIPipeDocumentation(name = "Parameters", description = "Contains algorithm parameters")
@JsonSerialize(using = ParametersData.Serializer.class)
@JsonDeserialize(using = ParametersData.Deserializer.class)
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.json file that stores the parameters. " +
        "The JSON data is an object with keys being the parameter keys. The value is an object with two " +
        "items <code>value</code> and <code>type-id</code>. <code>value</code> contains the serialized parameter value." +
        " <code>type-id</code> contains the standardized parameter type ID.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/parameters-data.schema.json")
public class ParametersData implements JIPipeData {

    private Map<String, Object> parameterData = new HashMap<>();

    public static ParametersData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(ParametersData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(storage.getFileSystemPath().resolve(name + ".json").toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        ParametersData data = new ParametersData();
        data.parameterData = new HashMap<>(parameterData);
        return data;
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        CachedParametersDataViewerWindow window = new CachedParametersDataViewerWindow(workbench, JIPipeDataTableDataSource.wrap(this, source), displayName);
        window.setVisible(true);
        SwingUtilities.invokeLater(window::reloadDisplayedData);
    }

    public Map<String, Object> getParameterData() {
        return parameterData;
    }

    public void setParameterData(Map<String, Object> parameterData) {
        this.parameterData = parameterData;
    }

    @Override
    public String toString() {
        return "Parameters (" + String.join(", ", parameterData.keySet()) + ")";
    }

    /**
     * Serializes {@link ParametersData}
     */
    public static class Serializer extends JsonSerializer<ParametersData> {
        @Override
        public void serialize(ParametersData value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            for (Map.Entry<String, Object> entry : value.parameterData.entrySet()) {
                gen.writeFieldName(entry.getKey());
                gen.writeStartObject();
                gen.writeObjectField("value", entry.getValue());
                gen.writeStringField("type-id", JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getClass()).getId());
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ParametersData> {
        @Override
        public ParametersData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            ParametersData parametersData = new ParametersData();
            for (String key : ImmutableList.copyOf(node.fieldNames())) {
                JsonNode entryNode = node.get(key);
                String typeId = entryNode.get("type-id").asText();
                JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoById(typeId);
                Object value = JsonUtils.getObjectMapper().readerFor(typeInfo.getFieldClass()).readValue(entryNode.get("value"));
                parametersData.parameterData.put(key, value);
            }
            return parametersData;
        }
    }
}
