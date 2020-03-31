package org.hkijena.acaq5;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates a dependency such as an extension or JSON extension
 */
@JsonDeserialize(as = ACAQMutableDependency.class)
public interface ACAQDependency extends ACAQParameterHolder, ACAQValidatable {
    /**
     * @return The dependency metadata
     */
    @JsonGetter("metadata")
    ACAQProjectMetadata getMetadata();

    @Override
    EventBus getEventBus();

    /**
     * @return The unique dependency ID
     */
    @JsonGetter("id")
    String getDependencyId();

    /**
     * @return The dependency version
     */
    @JsonGetter("version")
    String getDependencyVersion();

    /**
     * Gets the location of the JAR/JSON that defined the dependency
     *
     * @return location of the JAR/JSON that defined the dependency
     */
    Path getDependencyLocation();

    @Override
    void reportValidity(ACAQValidityReport report);

    /**
     * Exports the dependency to an HTML element (without the root tag)
     *
     * @param dependency Dependency instance
     * @return HTML element without HTML root tags
     */
    static String toHtmlElement(ACAQDependency dependency) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<u><strong>").append(dependency.getMetadata().getName()).append("</strong></u><br/>");
        stringBuilder.append("<i>").append(dependency.getDependencyId()).append("</i><br/><br/>");
        stringBuilder.append(dependency.getMetadata().getDescription()).append("<br/><br/>");
        stringBuilder.append("<strong>").append("Version ").append(dependency.getDependencyVersion()).append("</strong><br/>");
        stringBuilder.append("<strong>").append("By ").append(dependency.getMetadata().getAuthors()).append("</strong><br/>");
        if (dependency.getMetadata().getWebsite() != null && !dependency.getMetadata().getWebsite().isEmpty())
            stringBuilder.append("<strong>").append("URL <a href=\"").append(dependency.getMetadata().getWebsite()).append("\" target=\"_blank\">")
                    .append(dependency.getMetadata().getWebsite()).append("</a></strong><br/>");
        if (dependency.getMetadata().getLicense() != null && !dependency.getMetadata().getLicense().isEmpty())
            stringBuilder.append("<strong>").append("Licensed under ").append(dependency.getMetadata().getLicense()).append("</strong><br/>");
        if (dependency.getMetadata().getCitation() != null && !dependency.getMetadata().getCitation().isEmpty())
            stringBuilder.append("<strong>").append("Please cite: ").append(dependency.getMetadata().getCitation()).append("</strong><br/>");
        return stringBuilder.toString();
    }

    /**
     * Finds all dependencies that cannot be met
     *
     * @param dependencies List of dependencies to be checked. Only the ID will be checked.
     * @return Set of dependencies whose IDs are not registered
     */
    static Set<ACAQDependency> findUnsatisfiedDependencies(Set<ACAQDependency> dependencies) {
        Set<ACAQDependency> result = new HashSet<>();
        for (ACAQDependency dependency : dependencies) {
            boolean found = ACAQDefaultRegistry.getInstance().getRegisteredExtensions().stream().anyMatch(d -> d.getDependencyId().equals(dependency.getDependencyId()));
            if (!found)
                result.add(dependency);
        }
        return result;
    }
}
