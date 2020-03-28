package org.hkijena.acaq5.extensions.jsonextensionloader;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQProject;

import java.util.Set;
import java.util.stream.Collectors;

public class JsonExtensionRegistrationTask {

    private final Set<String> dependencyIds;
    private ACAQDefaultRegistry registry;
    private JsonNode jsonNode;

    public JsonExtensionRegistrationTask(ACAQDefaultRegistry registry, JsonNode jsonNode) {
        this.registry = registry;
        this.jsonNode = jsonNode;
        Set<ACAQDependency> dependencies = ACAQProject.loadDependenciesFromJson(jsonNode);
        this.dependencyIds = dependencies.stream().map(ACAQDependency::getDependencyId).collect(Collectors.toSet());
    }

    public boolean canRegister() {
        return registry.getRegisteredExtensionIds().containsAll(dependencyIds);
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }
}
