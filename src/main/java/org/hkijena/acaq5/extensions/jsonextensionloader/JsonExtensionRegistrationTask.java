package org.hkijena.acaq5.extensions.jsonextensionloader;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers a {@link org.hkijena.acaq5.ACAQJsonExtension}
 */
public class JsonExtensionRegistrationTask implements ACAQValidatable {

    private final Set<String> dependencyIds;
    private ACAQDefaultRegistry registry;
    private Path filePath;
    private JsonNode jsonNode;

    /**
     * @param registry the registry
     * @param filePath the path the JSON data came from. Has only informational value. Can be null.
     * @param jsonNode the JSON data node that contains the serialized extension
     */
    public JsonExtensionRegistrationTask(ACAQDefaultRegistry registry, Path filePath, JsonNode jsonNode) {
        this.registry = registry;
        this.filePath = filePath;
        this.jsonNode = jsonNode;
        Set<ACAQDependency> dependencies = ACAQProject.loadDependenciesFromJson(jsonNode);
        this.dependencyIds = dependencies.stream().map(ACAQDependency::getDependencyId).collect(Collectors.toSet());
    }

    /**
     * @return true if the extension can be registered
     */
    public boolean canRegister() {
        return registry.getRegisteredExtensionIds().containsAll(dependencyIds);
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (String dependencyId : dependencyIds) {
            if (!registry.getRegisteredExtensionIds().contains(dependencyId)) {
                report.reportIsInvalid("Dependency '" + dependencyId + "' is missing! Please ensure that the matching extension is installed. Otherwise you can try to open the extension" +
                        " '" + filePath + "' in the extension builder and save it to update dependencies.");
            }
        }

    }
}
