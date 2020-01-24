package org.hkijena.acaq5.api.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.acaq5.api.ACAQAlgorithm;

import java.io.IOException;

public class ACAQAlgorithmParametersSerializer extends JsonSerializer<ACAQAlgorithm> {
    @Override
    public void serialize(ACAQAlgorithm algorithm, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeTree(jsonGenerator.getCodec().createObjectNode());
    }
}
