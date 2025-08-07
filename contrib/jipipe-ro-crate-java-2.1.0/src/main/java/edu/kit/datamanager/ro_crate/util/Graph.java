package edu.kit.datamanager.ro_crate.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.stream.StreamSupport;

/**
 * Utility class for handling operations on RO-Crate graphs.
 * Provides methods to find entities by ID or type within a graph.
 * <p>
 * {@see JsonUtilFunctions}.
 */
public class Graph {

    private Graph() {
        // Private constructor to prevent instantiation
    }

    /**
     * Finds an entity in the graph by its ID.
     *
     * @param graph The JSON node representing the graph.
     * @param id    The ID of the entity to find.
     * @return The entity as a JsonNode if found, null otherwise.
     */
    public static JsonNode findEntityById(JsonNode graph, String id) {
        for (JsonNode entity : graph) {
            if (entity.has("@id") && entity.get("@id").asText().equals(id)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Finds an entity in the graph by its type.
     *
     * @param graph The JSON node representing the graph.
     * @param type  The type of the entity to find.
     * @return The entity as a JsonNode if found, null otherwise.
     */
    public static JsonNode findEntityByType(JsonNode graph, String type) {
        return StreamSupport.stream(graph.spliterator(), false)
                .filter(entity -> entity.path("@type").asText().equals(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds all entities in the graph by their type.
     *
     * @param graph The JSON node representing the graph.
     * @param type  The type of the entities to find.
     * @return An array of JsonNode containing all entities of the specified type.
     */
    public static JsonNode[] findEntitiesByType(JsonNode graph, String type) {
        return StreamSupport.stream(graph.spliterator(), false)
                .filter(entity -> entity.path("@type").asText().equals(type))
                .toArray(JsonNode[]::new);
    }
}
