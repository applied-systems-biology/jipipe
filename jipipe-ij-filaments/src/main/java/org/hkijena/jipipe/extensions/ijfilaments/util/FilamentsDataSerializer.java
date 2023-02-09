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
    public void serialize(FilamentsData FilamentsData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        BiMap<UUID, FilamentVertex> vertexMap = HashBiMap.create();
        jsonGenerator.writeObjectFieldStart("vertices");
        for (FilamentVertex vertex : FilamentsData.vertexSet()) {
            UUID uuid = UUID.randomUUID();
            vertexMap.put(uuid, vertex);
            jsonGenerator.writeObjectField(uuid.toString(), vertex);
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.writeArrayFieldStart("edges");
        for (FilamentEdge edge : FilamentsData.edgeSet()) {
            FilamentVertex edgeSource = FilamentsData.getEdgeSource(edge);
            FilamentVertex edgeTarget = FilamentsData.getEdgeTarget(edge);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("source", vertexMap.inverse().get(edgeSource).toString());
            jsonGenerator.writeStringField("target", vertexMap.inverse().get(edgeTarget).toString());
            jsonGenerator.writeObjectField("data", edge);
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
