package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;

import java.io.IOException;
import java.util.UUID;

public class FilamentsDataSerializer extends JsonSerializer<FilamentsData> {
    @Override
    public void serialize(FilamentsData filamentsData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectFieldStart("vertices");
        for (FilamentVertex vertex : filamentsData.vertexSet()) {
            jsonGenerator.writeObjectField(vertex.getUuid().toString(), vertex);
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.writeArrayFieldStart("edges");
        for (FilamentEdge edge : filamentsData.edgeSet()) {
            FilamentVertex edgeSource = filamentsData.getEdgeSource(edge);
            FilamentVertex edgeTarget = filamentsData.getEdgeTarget(edge);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("source", edgeSource.getUuid().toString());
            jsonGenerator.writeStringField("target", edgeTarget.getUuid().toString());
            jsonGenerator.writeObjectField("data", edge);
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
