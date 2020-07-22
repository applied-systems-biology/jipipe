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

package org.hkijena.jipipe;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates a dependency such as an extension or JSON extension
 */
@JsonDeserialize(as = JIPipeMutableDependency.class)
public interface JIPipeDependency extends JIPipeParameterCollection, JIPipeValidatable {
    /**
     * @return The dependency metadata
     */
    @JsonGetter("metadata")
    JIPipeMetadata getMetadata();

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

    /**
     * List of ImageJ update sites that are dependencies
     *
     * @return the list of update sites
     */
    @JsonSetter("ij:update-sites")
    default List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Collections.emptyList();
    }

    @Override
    void reportValidity(JIPipeValidityReport report);

    /**
     * Exports the dependency to an HTML element (without the root tag)
     *
     * @param dependency Dependency instance
     * @return HTML element without HTML root tags
     */
    static String toHtmlElement(JIPipeDependency dependency) {
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
    static Set<JIPipeDependency> findUnsatisfiedDependencies(Set<JIPipeDependency> dependencies) {
        Set<JIPipeDependency> result = new HashSet<>();
        for (JIPipeDependency dependency : dependencies) {
            boolean found = JIPipeDefaultRegistry.getInstance().getRegisteredExtensions().stream().anyMatch(d -> d.getDependencyId().equals(dependency.getDependencyId()));
            if (!found)
                result.add(dependency);
        }
        return result;
    }
}
