package org.hkijena.acaq5;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;

import java.nio.file.Path;

/**
 * A JSON-serializable {@link ACAQDependency}.
 * {@link ACAQDependency} is deserialize to this type.
 */
public class ACAQMutableDependency implements ACAQDependency {
    private ACAQProjectMetadata metadata = new ACAQProjectMetadata();
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
        this.metadata = new ACAQProjectMetadata(other.getMetadata());
        this.dependencyId = other.getDependencyId();
        this.dependencyVersion = other.getDependencyVersion();
    }

    @Override
    @JsonGetter("metadata")
    @ACAQSubParameters("metadata")
    @ACAQDocumentation(name = "Metadata")
    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     *
     * @param metadata The metadata
     */
    @JsonSetter("metadata")
    public void setMetadata(ACAQProjectMetadata metadata) {
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
