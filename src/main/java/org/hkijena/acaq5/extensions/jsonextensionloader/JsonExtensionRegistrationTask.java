package org.hkijena.acaq5.extensions.jsonextensionloader;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQProject;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonExtensionRegistrationTask {

    private final Set<String> dependencyIds;
    private ACAQDefaultRegistry registry;
    private Path filePath;
    private JsonNode jsonNode;

    public JsonExtensionRegistrationTask(ACAQDefaultRegistry registry, Path filePath, JsonNode jsonNode) {
        this.registry = registry;
        this.filePath = filePath;
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

    public Path getFilePath() {
        return filePath;
    }
}
