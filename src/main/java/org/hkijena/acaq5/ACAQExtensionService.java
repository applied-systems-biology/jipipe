package org.hkijena.acaq5;

import org.scijava.service.Service;

import java.net.URL;
import java.util.List;

public interface ACAQExtensionService extends Service {

    String getName();

    String getDescription();

    List<String> getAuthors();

    String getURL();

    String getLicense();

    URL getIconURL();

    /**
     * Registers custom modules into ACAQ5
     *
     * @param registryService
     */
    void register(ACAQRegistryService registryService);
}
