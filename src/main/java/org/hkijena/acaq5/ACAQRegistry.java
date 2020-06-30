/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQImageJAdapterRegistry;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQSettingsRegistry;
import org.hkijena.acaq5.api.registries.ACAQTableRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIImageJDatatypeAdapterRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIMenuServiceRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.scijava.service.Service;

import java.util.List;
import java.util.Set;

/**
 * Contains all ACAQ resources
 */
public interface ACAQRegistry extends Service, ACAQValidatable {

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
