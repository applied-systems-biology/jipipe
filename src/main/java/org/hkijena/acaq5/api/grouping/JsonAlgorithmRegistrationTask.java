package org.hkijena.acaq5.api.grouping;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDefaultAlgorithmRegistrationTask;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.ResourceUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Registers a {@link JsonAlgorithmDeclaration}
 */
public class JsonAlgorithmRegistrationTask extends ACAQDefaultAlgorithmRegistrationTask {

    private final JsonNode jsonNode;
    private final ACAQJsonExtension source;
    private boolean alreadyRegistered = false;

    /**
     * @param jsonNode The JSON serialized graph wrapper algorithm
     * @param source   dependency that registers the algorithm
     */
    public JsonAlgorithmRegistrationTask(JsonNode jsonNode, ACAQJsonExtension source) {
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
            JsonAlgorithmDeclaration declaration = JsonUtils.getObjectMapper().readerFor(JsonAlgorithmDeclaration.class).readValue(jsonNode);
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

