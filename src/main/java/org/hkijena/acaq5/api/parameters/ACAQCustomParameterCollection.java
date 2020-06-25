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

package org.hkijena.acaq5.api.parameters;

import java.util.Collections;
import java.util.Map;

/**
 * If a class inherits from this interface, reflection-based parameter discovery is replaced by
 * the getCustomParameters() method result.
 */
public interface ACAQCustomParameterCollection extends ACAQParameterCollection {
    /**
     * Returns all parameters
     * Please note that those parameters must be sourced in this collection.
     * Use getChildParameterCollections() to add those
     *
     * @return Map from parameter ID to its access. The ID is not necessarily equal to {@link ACAQParameterAccess}.getKey()
     */
    Map<String, ACAQParameterAccess> getParameters();

    /**
     * Returns all sub-parameter collections
     *
     * @return Map from unique collection ID to the collection.
     */
    default Map<String, ACAQParameterCollection> getChildParameterCollections() {
        return Collections.emptyMap();
    }
}
