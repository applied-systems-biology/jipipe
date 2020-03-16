package org.hkijena.acaq5.extension.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistrationTask;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extension.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphWrapperAlgorithmRegistrationTask implements ACAQAlgorithmRegistrationTask {

    private JsonNode jsonNode;
    private Set<String> dependencyAlgorithms;

    /**
     *
     * @param jsonNode The JSON serialized graph wrapper algorithm
     */
    public GraphWrapperAlgorithmRegistrationTask(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
        findDependencyAlgorithms();
    }

    private void findDependencyAlgorithms() {
        dependencyAlgorithms = new HashSet<>();
        JsonNode graphNodesNode = jsonNode.get("graph").get("nodes");
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(graphNodesNode.fields())) {
            JsonNode declarationIdNode = entry.getValue().get("acaq:algorithm-type");
            dependencyAlgorithms.add(declarationIdNode.asText());
        }
    }

    @Override
    public void register() {
        try {
            GraphWrapperAlgorithmDeclaration declaration = JsonUtils.getObjectMapper().readerFor(GraphWrapperAlgorithmDeclaration.class).readValue(jsonNode);
            ACAQAlgorithmRegistry.getInstance().register(declaration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canRegister() {
        return dependencyAlgorithms.stream().allMatch(id -> ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id));
    }
}
