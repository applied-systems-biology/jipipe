package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQAPIService;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;

public interface ACAQUIService extends ACAQAPIService {
    ACAQUIDatatypeRegistry getUIDatatypeRegistry();

    ACAQUIParametertypeRegistry getUIParametertypeRegistry();

    ACAQUITraitRegistry getUITraitRegistry();
}
