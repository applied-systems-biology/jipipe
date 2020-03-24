package org.hkijena.acaq5;

import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.ui.registries.ACAQPlotBuilderRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.scijava.service.Service;

public interface ACAQRegistry extends Service {

    ACAQAlgorithmRegistry getAlgorithmRegistry();

    ACAQDatatypeRegistry getDatatypeRegistry();

    ACAQTraitRegistry getTraitRegistry();

    ACAQUIDatatypeRegistry getUIDatatypeRegistry();

    ACAQUIParametertypeRegistry getUIParametertypeRegistry();

    ACAQUITraitRegistry getUITraitRegistry();

    ACAQPlotBuilderRegistry getPlotBuilderRegistry();
}
