package org.hkijena.acaq5;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.registries.*;
import org.hkijena.acaq5.ui.registries.*;
import org.scijava.service.Service;

import java.util.List;
import java.util.Set;

/**
 * Contains all ACAQ resources
 */
public interface ACAQRegistry extends Service, ACAQValidatable {

    /**
     * @return Registry for table analyzer operations
     */
    ACAQTableAnalyzerUIOperationRegistry getTableAnalyzerUIOperationRegistry();

    /**
     * @return The event bus
     */
    EventBus getEventBus();

    /**
     * @return Adapters between ACAQ5 and ImageJ
     */
    ACAQImageJAdapterRegistry getImageJDataAdapterRegistry();

    /**
     * @return Registered extensions
     */
    List<ACAQDependency> getRegisteredExtensions();

    /**
     * @return Registered algorithms
     */
    ACAQAlgorithmRegistry getAlgorithmRegistry();

    /**
     * @return Registered data types
     */
    ACAQDatatypeRegistry getDatatypeRegistry();

    /**
     * @return Registered annotation types
     */
    ACAQTraitRegistry getTraitRegistry();

    /**
     * @return Registered data type UIs
     */
    ACAQUIDatatypeRegistry getUIDatatypeRegistry();

    /**
     * @return Registered parameters
     */
    ACAQParameterTypeRegistry getParameterTypeRegistry();

    /**
     * @return Registered parameter UIs
     */
    ACAQUIParameterTypeRegistry getUIParameterTypeRegistry();

    /**
     * @return Registered annotation UIs
     */
    ACAQUITraitRegistry getUITraitRegistry();

    /**
     * @return Registered ImageJ adapter UIs
     */
    ACAQUIImageJDatatypeAdapterRegistry getUIImageJDatatypeAdapterRegistry();

    /**
     * @return Registered menus
     */
    ACAQUIMenuServiceRegistry getUIMenuServiceRegistry();


    ACAQSettingsRegistry getSettingsRegistry();

    ACAQTableRegistry getTableRegistry();

    ACAQUIAlgorithmRegistry getUIAlgorithmRegistry();

    /**
     * @return Registered extension IDs
     */
    Set<String> getRegisteredExtensionIds();

    /**
     * Finds an extension by its ID
     *
     * @param dependencyId The extension ID
     * @return The extension. Null if its not found.
     */
    ACAQDependency findExtensionById(String dependencyId);
}
