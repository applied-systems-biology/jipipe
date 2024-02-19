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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A JSON-serializable {@link JIPipeDependency}.
 * {@link JIPipeDependency} is deserialize to this type.
 */
public class JIPipeMutableDependency implements JIPipeDependency {
    private JIPipeMetadata metadata = new JIPipeMetadata();
    private String dependencyId;
    private String dependencyVersion = "1.0.0";
    private List<JIPipeImageJUpdateSiteDependency> imageJUpdateSiteDependencies = new ArrayList<>();

    private Set<JIPipeDependency> dependencies = new HashSet<>();
    private List<JIPipeImageJUpdateSiteDependency> imageJUpdateSites = new ArrayList<>();

    /**
     * Creates a new instance
     */
    public JIPipeMutableDependency() {
    }

    public JIPipeMutableDependency(String dependencyId, String dependencyVersion, String name) {
        this.dependencyId = dependencyId;
        this.dependencyVersion = dependencyVersion;
        this.metadata.setName(name);
    }

    /**
     * Copies the dependency
     *
     * @param other the original
     */
    public JIPipeMutableDependency(JIPipeDependency other) {
        this.metadata = new JIPipeMetadata(other.getMetadata());
        this.dependencyId = other.getDependencyId();
        this.dependencyVersion = other.getDependencyVersion();
        for (JIPipeDependency dependency : other.getDependencies()) {
            this.dependencies.add(new JIPipeMutableDependency(dependency));
        }
    }

    @Override
    @JsonGetter("metadata")
    @JIPipeParameter("metadata")
    @SetJIPipeDocumentation(name = "Metadata")
    public JIPipeMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     *
     * @param metadata The metadata
     */
    @JsonSetter("metadata")
    public void setMetadata(JIPipeMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    @JsonGetter("id")
    @JIPipeParameter("id")
    @SetJIPipeDocumentation(name = "ID", description = "Unique identifier for this extension")
    public String getDependencyId() {
        return dependencyId;
    }

    /**
     * Sets the ID
     *
     * @param dependencyId The ID
     */
    @JsonSetter("id")
    @JIPipeParameter("id")
    public void setDependencyId(String dependencyId) {
        this.dependencyId = dependencyId;
    }

    @Override
    @JsonGetter("version")
    @JIPipeParameter("version")
    @SetJIPipeDocumentation(name = "Version", description = "Extension version")
    public String getDependencyVersion() {
        return dependencyVersion;
    }

    /**
     * Sets the version
     *
     * @param dependencyVersion The version
     */
    @JsonSetter("version")
    @JIPipeParameter("version")
    public void setDependencyVersion(String dependencyVersion) {
        this.dependencyVersion = dependencyVersion;
    }

    @Override
    public Path getDependencyLocation() {
        return null;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {

    }

    @Override
    @JsonGetter("ij:update-site-dependencies")
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSiteDependencies() {
        return imageJUpdateSiteDependencies;
    }

    @JsonSetter("ij:update-site-dependencies")
    public void setImageJUpdateSiteDependencies(List<JIPipeImageJUpdateSiteDependency> imageJUpdateSiteDependencies) {
        this.imageJUpdateSiteDependencies = imageJUpdateSiteDependencies;
    }

    @Override
    @JsonGetter("ij:update-site-providers")
    public List<JIPipeImageJUpdateSiteDependency> getImageJUpdateSites() {
        return imageJUpdateSites;
    }

    @JsonSetter("ij:update-site-providers")
    public void setImageJUpdateSites(List<JIPipeImageJUpdateSiteDependency> imageJUpdateSites) {
        this.imageJUpdateSites = imageJUpdateSites;
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return dependencies;
    }

    @JsonSetter("dependencies")
    public void setDependencies(Set<JIPipeDependency> dependencies) {
        this.dependencies = dependencies;
    }
}
