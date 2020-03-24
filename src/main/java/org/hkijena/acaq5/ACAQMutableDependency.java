package org.hkijena.acaq5;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQProjectMetadata;

public class ACAQMutableDependency implements ACAQDependency {
    private ACAQProjectMetadata metadata = new ACAQProjectMetadata();
    private String dependencyId;
    private String dependencyVersion;
    private EventBus eventBus = new EventBus();

    @Override
    @JsonGetter("metadata")
    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

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
    public String getDependencyId() {
        return dependencyId;
    }

    @JsonSetter("id")
    public void setDependencyId(String dependencyId) {
        this.dependencyId = dependencyId;
    }

    @Override
    @JsonGetter("version")
    public String getDependencyVersion() {
        return dependencyVersion;
    }

    @JsonSetter("version")
    public void setDependencyVersion(String dependencyVersion) {
        this.dependencyVersion = dependencyVersion;
    }
}
