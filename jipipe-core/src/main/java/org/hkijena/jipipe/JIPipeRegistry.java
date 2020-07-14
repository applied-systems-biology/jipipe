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

package org.hkijena.jipipe;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.ui.registries.*;
import org.scijava.service.Service;

import java.util.List;
import java.util.Set;

/**
 * Contains all JIPipe resources
 */
public interface JIPipeRegistry extends Service, JIPipeValidatable {

    /**
     * @return The event bus
     */
    EventBus getEventBus();

    /**
     * @return Adapters between JIPipe and ImageJ
     */
    JIPipeImageJAdapterRegistry getImageJDataAdapterRegistry();

    /**
     * @return Registered extensions
     */
    List<JIPipeDependency> getRegisteredExtensions();

    /**
     * @return Registered algorithms
     */
    JIPipeNodeRegistry getAlgorithmRegistry();

    /**
     * @return Registered data types
     */
    JIPipeDatatypeRegistry getDatatypeRegistry();

    /**
     * @return Registered data type UIs
     */
    JIPipeUIDatatypeRegistry getUIDatatypeRegistry();

    /**
     * @return Registered parameters
     */
    JIPipeParameterTypeRegistry getParameterTypeRegistry();

    /**
     * @return Registered parameter UIs
     */
    JIPipeUIParameterTypeRegistry getUIParameterTypeRegistry();

    /**
     * @return Registered ImageJ adapter UIs
     */
    JIPipeUIImageJDatatypeAdapterRegistry getUIImageJDatatypeAdapterRegistry();

    /**
     * @return Registered menus
     */
    JIPipeUIMenuServiceRegistry getUIMenuServiceRegistry();


    JIPipeSettingsRegistry getSettingsRegistry();

    JIPipeTableRegistry getTableRegistry();

    JIPipeUIAlgorithmRegistry getUIAlgorithmRegistry();

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
    JIPipeDependency findExtensionById(String dependencyId);
}
