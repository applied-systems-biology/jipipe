package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQAPIService;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

public interface ACAQUIService extends ACAQAPIService {
    ACAQUIDatatypeRegistry getUIDatatypeRegistry();
}
