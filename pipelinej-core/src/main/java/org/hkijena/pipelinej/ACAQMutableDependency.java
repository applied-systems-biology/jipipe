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

package org.hkijena.pipelinej;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQMetadata;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;

import java.nio.file.Path;

/**
 * A JSON-serializable {@link ACAQDependency}.
 * {@link ACAQDependency} is deserialize to this type.
 */
public class ACAQMutableDependency implements ACAQDependency {
    private ACAQMetadata metadata = new ACAQMetadata();
    private String dependencyId;
    private String dependencyVersion = "1.0.0";
    private EventBus eventBus = new EventBus();

    /**
     * Creates a new instance
     */
    public ACAQMutableDependency() {
    }

    /**
     * Copies the dependency
     *
     * @param other the original
     */
    public ACAQMutableDependency(ACAQDependency other) {
        this.metadata = new ACAQMetadata(other.getMetadata());
        this.dependencyId = other.getDependencyId();
        this.dependencyVersion = other.getDependencyVersion();
    }

    @Override
    @JsonGetter("metadata")
    @ACAQParameter("metadata")
    @ACAQDocumentation(name = "Metadata")
    public ACAQMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     *
     * @param metadata The metadata
     */
    @JsonSetter("metadata")
    public void setMetadata(ACAQMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    @JsonGetter("id")
    @ACAQParameter("id")
    @ACAQDocumentation(name = "ID", description = "Unique identifier for this extension")
    public String getDependencyId() {
        return dependencyId;
    }

    /**
     * Sets the ID
     *
     * @param dependencyId The ID
     */
    @JsonSetter("id")
    @ACAQParameter("id")
    public void setDependencyId(String dependencyId) {
        this.dependencyId = dependencyId;
    }

    @Override
    @JsonGetter("version")
    @ACAQParameter("version")
    @ACAQDocumentation(name = "Version", description = "Extension version")
    public String getDependencyVersion() {
        return dependencyVersion;
    }

    /**
     * Sets the version
     *
     * @param dependencyVersion The version
     */
    @JsonSetter("version")
    @ACAQParameter("version")
    public void setDependencyVersion(String dependencyVersion) {
        this.dependencyVersion = dependencyVersion;
    }

    @Override
    public Path getDependencyLocation() {
        return null;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
