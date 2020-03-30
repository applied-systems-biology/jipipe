package org.hkijena.acaq5;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQImageJAdapterRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.ui.registries.*;
import org.scijava.service.Service;

import java.util.List;

public interface ACAQRegistry extends Service, ACAQValidatable {

    EventBus getEventBus();

    ACAQImageJAdapterRegistry getImageJDataAdapterRegistry();

    List<ACAQDependency> getRegisteredExtensions();

    ACAQAlgorithmRegistry getAlgorithmRegistry();

    ACAQDatatypeRegistry getDatatypeRegistry();

    ACAQTraitRegistry getTraitRegistry();

    ACAQUIDatatypeRegistry getUIDatatypeRegistry();

    ACAQUIParametertypeRegistry getUIParametertypeRegistry();

    ACAQUITraitRegistry getUITraitRegistry();

    ACAQPlotBuilderRegistry getPlotBuilderRegistry();

    ACAQUIImageJDatatypeAdapterRegistry getUIImageJDatatypeAdapterRegistry();
}
