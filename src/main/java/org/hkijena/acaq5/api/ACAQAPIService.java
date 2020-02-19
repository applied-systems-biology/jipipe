package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.scijava.service.Service;

public interface ACAQAPIService extends Service {
    ACAQAlgorithmRegistry getAlgorithmRegistry();
    ACAQDatatypeRegistry getDatatypeRegistry();
    ACAQTraitRegistry getTraitRegistry();
}
