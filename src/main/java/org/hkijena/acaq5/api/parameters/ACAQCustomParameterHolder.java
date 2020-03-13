package org.hkijena.acaq5.api.parameters;

import java.util.Map;

/**
 * If a class inherits from this interface, reflection-based parameter discovery is replaced by
 * the getCustomParameters() method result.
 */
public interface ACAQCustomParameterHolder {
    /**
     * Returns all parameters
     * @return
     */
    Map<String, ACAQParameterAccess> getCustomParameters();
}
