/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.extensions.jsonextensionloader;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.pipelinej.ACAQDefaultRegistry;
import org.hkijena.pipelinej.ACAQDependency;
import org.hkijena.pipelinej.api.ACAQProject;
import org.hkijena.pipelinej.api.ACAQValidatable;
import org.hkijena.pipelinej.api.ACAQValidityReport;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers a {@link org.hkijena.pipelinej.ACAQJsonExtension}
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
                report.reportIsInvalid("A dependency is missing!",
                        "Dependency '" + dependencyId + "' is missing!",
                        "Please ensure that the matching extension is installed. Otherwise you can try to open the extension" +
                                " '" + filePath + "' in the extension builder and save it to update dependencies.",
                        this);
            }
        }

    }
}
