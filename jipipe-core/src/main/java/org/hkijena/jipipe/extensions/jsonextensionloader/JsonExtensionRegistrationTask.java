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

package org.hkijena.jipipe.extensions.jsonextensionloader;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeIssueReport;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers a {@link org.hkijena.jipipe.JIPipeJsonExtension}
 */
public class JsonExtensionRegistrationTask implements JIPipeValidatable {

    private final Set<String> dependencyIds;
    private JIPipe registry;
    private Path filePath;
    private JsonNode jsonNode;

    /**
     * @param registry the registry
     * @param filePath the path the JSON data came from. Has only informational value. Can be null.
     * @param jsonNode the JSON data node that contains the serialized extension
     */
    public JsonExtensionRegistrationTask(JIPipe registry, Path filePath, JsonNode jsonNode) {
        this.registry = registry;
        this.filePath = filePath;
        this.jsonNode = jsonNode;
        Set<JIPipeDependency> dependencies = JIPipeProject.loadDependenciesFromJson(jsonNode);
        this.dependencyIds = dependencies.stream().map(JIPipeDependency::getDependencyId).collect(Collectors.toSet());
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
    public void reportValidity(JIPipeIssueReport report) {
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
