package org.hkijena.acaq5.extensions.multiparameters.datatypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.utils.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the parameters of an algorithm
 */
@ACAQDocumentation(name = "Parameters", description = "Contains algorithm parameters")
@JsonSerialize(using = ParametersData.Serializer.class)
public class ParametersData implements ACAQData {

    private Map<String, Object> parameterData = new HashMap<>();

    @Override
    public void saveTo(Path storageFilePath, String name) {
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(storageFilePath.resolve(name + ".json").toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ACAQData duplicate() {
        ParametersData data = new ParametersData();
        data.parameterData = new HashMap<>(parameterData);
        return data;
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {
        try {
            Path outputFile = Files.createTempFile("ACAQTempParameters-" + displayName, ".json");
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile.toFile(), this);
            Desktop.getDesktop().open(outputFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            gen.writeObjectField("data", value.parameterData);
            gen.writeEndObject();
        }
    }
}
