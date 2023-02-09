package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class FilamentsDataDeserializer extends JsonDeserializer<FilamentsData> {
    @Override
    public FilamentsData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        FilamentsData graph = new FilamentsData();
        JsonNode root = jsonParser.readValueAsTree();
        BiMap<String, FilamentVertex> vertexMap = HashBiMap.create();
        for (Map.Entry<String, JsonNode> vertexEntry : ImmutableList.copyOf(root.get("vertices").fields())) {
            FilamentVertex vertex = JsonUtils.getObjectMapper().readerFor(FilamentVertex.class).readValue(vertexEntry.getValue());
            graph.addVertex(vertex);
            vertexMap.put(vertexEntry.getKey(), vertex);
        }
        for (JsonNode edgeEntry : ImmutableList.copyOf(root.get("edges").elements())) {
            String sourceId = edgeEntry.get("source").textValue();
            String targetId = edgeEntry.get("target").textValue();
            FilamentEdge edge = JsonUtils.getObjectMapper().readerFor(FilamentEdge.class).readValue(edgeEntry.get("data"));
            graph.addEdge(vertexMap.get(sourceId), vertexMap.get(targetId), edge);
        }
        return graph;
    }
}
