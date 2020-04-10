package org.hkijena.acaq5.extensions.multiparameters.datatypes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the parameters of an algorithm
 */
@ACAQDocumentation(name = "Parameters", description = "Contains algorithm parameters")
@JsonSerialize(using = ParametersData.Serializer.class)
public class ParametersData implements ACAQData {

    private ACAQAlgorithmDeclaration algorithmDeclaration;
    private Map<String, Object> parameterData = new HashMap<>();

    @Override
    public void saveTo(Path storageFilePath, String name) {
        try {
            JsonUtils.getObjectMapper().writeValue(storageFilePath.resolve(name + ".json").toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ACAQData duplicate() {
        ParametersData data = new ParametersData();
        data.algorithmDeclaration = algorithmDeclaration;
        data.parameterData = new HashMap<>(parameterData);
        return data;
    }

    public ACAQAlgorithmDeclaration getAlgorithmDeclaration() {
        return algorithmDeclaration;
    }

    public void setAlgorithmDeclaration(ACAQAlgorithmDeclaration algorithmDeclaration) {
        this.algorithmDeclaration = algorithmDeclaration;
    }

    public Map<String, Object> getParameterData() {
        return parameterData;
    }

    public void setParameterData(Map<String, Object> parameterData) {
        this.parameterData = parameterData;
    }

    /**
     * Serializes {@link ParametersData}
     */
    public static class Serializer extends JsonSerializer<ParametersData> {
        @Override
        public void serialize(ParametersData value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartObject();
            gen.writeStringField("algorithm", value.algorithmDeclaration.getId());
            gen.writeObjectField("data", value.parameterData);
            gen.writeEndObject();
        }
    }
}
