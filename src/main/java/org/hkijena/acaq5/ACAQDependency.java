package org.hkijena.acaq5;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;

/**
 * Encapsulates a dependency such as an extension or JSON extension
 */
public interface ACAQDependency extends ACAQParameterHolder {
    ACAQProjectMetadata getMetadata();

    @Override
    EventBus getEventBus();

    String getDependencyId();

    String getDependencyVersion();
}
