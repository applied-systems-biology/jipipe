package org.hkijena.acaq5;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.scijava.service.AbstractService;

import java.net.URL;

public abstract class ACAQDefaultJavaExtension extends AbstractService implements ACAQJavaExtension {

    private EventBus eventBus = new EventBus();
    private ACAQProjectMetadata metadata;

    public ACAQDefaultJavaExtension() {
        metadata = new ACAQProjectMetadata();
        metadata.setName(getName());
        metadata.setDescription(getDescription());
        metadata.setAuthors(getAuthors());
        metadata.setCitation(getCitation());
        metadata.setLicense(getLicense());
        metadata.setWebsite(getWebsite());
    }

    public abstract String getCitation();

    @Override
    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getAuthors();

    public abstract String getWebsite();

    public abstract String getLicense();

    public abstract URL getLogo();
}
