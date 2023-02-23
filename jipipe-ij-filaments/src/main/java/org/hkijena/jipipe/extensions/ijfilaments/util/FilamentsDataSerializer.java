package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

import java.io.IOException;

public class FilamentsDataSerializer extends JsonSerializer<Filaments3DData> {
    @Override
    public void serialize(Filaments3DData filamentsData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
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
