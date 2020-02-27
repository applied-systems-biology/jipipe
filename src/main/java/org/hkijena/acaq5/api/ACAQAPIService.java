package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.scijava.service.Service;

/**
 * Interface for the API extension service
 */
public interface ACAQAPIService extends Service {
    ACAQAlgorithmRegistry getAlgorithmRegistry();
    ACAQDatatypeRegistry getDatatypeRegistry();
    ACAQTraitRegistry getTraitRegistry();

}
