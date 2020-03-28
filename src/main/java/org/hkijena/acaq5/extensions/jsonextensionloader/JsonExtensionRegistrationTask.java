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

public class JsonExtensionRegistrationTask implements ACAQValidatable {

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
