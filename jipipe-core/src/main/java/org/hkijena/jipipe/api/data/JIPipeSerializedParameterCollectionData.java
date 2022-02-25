package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.strings.JsonData;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonDeserializable;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A base class for {@link JIPipeData} that is serialized from/to JSON for convenience
 * Only valid {@link org.hkijena.jipipe.api.parameters.JIPipeParameter} definitions are stored.
 * Ensure that this data type has a copy constructor for the duplicate() function.
 * You also still need to add the proper {@link org.hkijena.jipipe.api.JIPipeDocumentation} annotation and
 * the JIPipeData importFrom(Path) static function.
 * Unlike {@link JIPipeSerializedJsonObjectData}, this data is serialized in a way to fully restore its data type during deserialization,
 * removing the requirement of storing the data type ID within the JSON.
 */
@JIPipeDataStorageDocumentation(humanReadableDescription = "A JSON file that contains the serialized data",
jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
@JsonSerialize(using = JIPipeSerializedParameterCollectionData.Serializer.class)
@JsonDeserialize(using = JIPipeSerializedParameterCollectionData.Deserializer.class)
public abstract class JIPipeSerializedParameterCollectionData implements JIPipeData, JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();

    public JIPipeSerializedParameterCollectionData() {

    }

    public JIPipeSerializedParameterCollectionData(JIPipeSerializedParameterCollectionData other) {

    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static JIPipeData importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storagePath, ".json");
        return JsonUtils.readFromFile(targetFile, JIPipeSerializedParameterCollectionData.class);
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        JsonUtils.saveToFile(this, storageFilePath.resolve(StringUtils.orElse(name, "data") + ".json"));
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
            ParameterUtils.deserializeParametersFromJson(data, parameterData, new JIPipeIssueReport());
            return data;
        }
    }
}
