package org.hkijena.acaq5.extensions.standardalgorithms.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.DefaultACAQAlgorithmRegistrationTask;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.Map;

public class GraphWrapperAlgorithmRegistrationTask extends DefaultACAQAlgorithmRegistrationTask {

    private JsonNode jsonNode;

    /**
     * @param jsonNode The JSON serialized graph wrapper algorithm
     */
    public GraphWrapperAlgorithmRegistrationTask(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
        findDependencyAlgorithms();
        findDependencyTraits("preferred-traits");
        findDependencyTraits("unwanted-traits");
        findDependencyTraits("added-traits");
        findDependencyTraits("removed-traits");
    }

    private void findDependencyTraits(String key) {
        JsonNode node = jsonNode.path(key);
        if (!node.isMissingNode()) {
            for (JsonNode entry : ImmutableList.copyOf(node.elements())) {
                getDependencyTraitIds().add(entry.asText());
            }
        }
    }

    private void findDependencyAlgorithms() {
        JsonNode graphNodesNode = jsonNode.get("graph").get("nodes");
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(graphNodesNode.fields())) {
            JsonNode declarationIdNode = entry.getValue().get("acaq:algorithm-type");
            getDependencyAlgorithmIds().add(declarationIdNode.asText());
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
}

