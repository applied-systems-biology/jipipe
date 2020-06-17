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
