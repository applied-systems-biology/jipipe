package org.hkijena.acaq5.api.parameters;

import java.util.Map;

/**
 * If a class inherits from this interface, reflection-based parameter discovery is replaced by
 * the getCustomParameters() method result.
 */
public interface ACAQCustomParameterCollection extends ACAQParameterCollection {
    /**
     * Returns all parameters
     *
     * @return Map from parameter ID to its access. The ID is not necessarily equal to {@link ACAQParameterAccess}.getKey()
     */
    Map<String, ACAQParameterAccess> getParameters();
}
