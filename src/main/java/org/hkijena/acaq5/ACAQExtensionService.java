package org.hkijena.acaq5;

import org.scijava.service.Service;

public interface ACAQExtensionService extends Service {
    /**
     * Registers custom modules into ACAQ5
     * @param registryService
     */
    void register(ACAQRegistryService registryService);
}
