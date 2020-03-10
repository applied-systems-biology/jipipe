package org.hkijena.acaq5.api.parameters;

import java.util.Map;

public interface ACAQDynamicParameterHolder {
    Map<String, ACAQParameterAccess> getDynamicParameters();
}
