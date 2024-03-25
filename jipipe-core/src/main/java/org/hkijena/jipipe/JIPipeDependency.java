/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;

import java.nio.file.Path;
import java.util.*;

/**
 * Encapsulates a dependency such as an extension or JSON extension
 */
@JsonDeserialize(as = JIPipeMutableDependency.class)
public interface JIPipeDependency extends JIPipeValidatable {
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
     * Flattens the hierarchy of dependencies into a list and removes large metadata (e.g. thumbnails)
     *
     * @param dependencies the dependencies
     * @param minimize     whether heavy data (e.g. thumbnails should be removed)
     * @return simplified dependencies
     */
    static Set<JIPipeDependency> simplifyAndMinimize(Set<JIPipeDependency> dependencies, boolean minimize) {
        Set<JIPipeDependency> result = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Stack<JIPipeDependency> stack = new Stack<>();
        stack.addAll(dependencies);
        while (!stack.isEmpty()) {
            JIPipeDependency dependency = stack.pop();
            if (visited.contains(dependency.getDependencyId()))
                continue;
            JIPipeMutableDependency copy = new JIPipeMutableDependency(dependency);
            if (minimize) {
                copy.getMetadata().setThumbnail(new ImageParameter());
            }
            copy.setDependencies(new HashSet<>());
            result.add(copy);
            stack.addAll(dependency.getDependencies());
            visited.add(dependency.getDependencyId());
        }
        return result;
    }

    /**
     * @return The dependency metadata
     */
    @JsonGetter("metadata")
    JIPipeMetadata getMetadata();

    /**
     * @return The unique dependency ID
     */
    @JsonGetter("id")
    String getDependencyId();

    /**
     * The list of alternative IDs that are covered by this dependency
     * @return alternative IDs
     */
    @JsonGetter("provides")
    StringList getDependencyProvides();

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
    @JsonGetter("ij:update-site-dependencies")
    default List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return Collections.emptyList();
    }

    /**
     * Gets a flat list of all dependencies (no nested dependencies)
     * This method returns the dependencies to be stored into JSON
     * {@link JIPipeMutableDependency} instances are returned instead of the original type and the thumbnail metadata is removed
     *
     * @return flat list of dependencies
     */
    @JsonGetter("dependencies")
    default Set<JIPipeDependency> getAllDependencies() {
        return simplifyAndMinimize(getDependencies(), true);
    }

    /**
     * Gets a flat list of all dependencies (no nested dependencies)
     * This method returns the dependencies to be stored into JSON
     * {@link JIPipeMutableDependency} instances are returned instead of the original type and the thumbnail metadata is removed
     *
     * @param minimize whether heavy data (e.g. thumbnails should be removed)
     * @return flat list of dependencies
     */
    default Set<JIPipeDependency> getAllDependencies(boolean minimize) {
        return simplifyAndMinimize(getDependencies(), minimize);
    }

    /**
     * Gets a flat list of all ImageJ update site dependencies (including transitive dependencies)
     * This method returns the dependencies to be stored into JSON
     *
     * @return flat list of all current and transitive dependencies
     */
    default Set<JIPipeImageJUpdateSiteDependency> getAllImageJUpdateSiteDependencies() {
        Set<JIPipeImageJUpdateSiteDependency> dependencies = new HashSet<>(getImageJUpdateSiteDependencies());
        for (JIPipeDependency dependency : getAllDependencies(true)) {
            dependencies.addAll(dependency.getImageJUpdateSiteDependencies());
        }
        return dependencies;
    }

    /**
     * List of JIPipe dependencies.
     * This can contain nested or even cyclic dependencies.
     * The storage of dependencies into JSON format is handled by getTraversedDependencies()
     *
     * @return the set of JIPipe dependencies
     */
    default Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    /**
     * List of ImageJ update sites that provide the dependency
     *
     * @return the list of update sites
     */
    @JsonGetter("ij:update-site-providers")
    default List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSites() {
        return Collections.emptyList();
    }

    @Override
    void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report);
}
