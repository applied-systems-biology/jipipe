package org.hkijena.acaq5.extensions.standardalgorithms.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDefaultAlgorithmRegistrationTask;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.ResourceUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Registers a {@link GraphWrapperAlgorithmDeclaration}
 */
public class GraphWrapperAlgorithmRegistrationTask extends ACAQDefaultAlgorithmRegistrationTask {

    private JsonNode jsonNode;
    private ACAQDependency source;
    private boolean alreadyRegistered = false;

    /**
     * @param jsonNode The JSON serialized graph wrapper algorithm
     * @param source   dependency that registers the algorithm
     */
    public GraphWrapperAlgorithmRegistrationTask(JsonNode jsonNode, ACAQDependency source) {
        this.jsonNode = jsonNode;
        this.source = source;
        findDependencyAlgorithms();
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
        if (alreadyRegistered)
            return;
        alreadyRegistered = true;
        try {
            GraphWrapperAlgorithmDeclaration declaration = JsonUtils.getObjectMapper().readerFor(GraphWrapperAlgorithmDeclaration.class).readValue(jsonNode);
            ACAQAlgorithmRegistry.getInstance().register(declaration, source);
            if (declaration.getIcon().getIconName() != null) {
                ACAQUIAlgorithmRegistry.getInstance().registerIcon(declaration,
                        ResourceUtils.getPluginResource("icons/algorithms/" + declaration.getIcon().getIconName()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

