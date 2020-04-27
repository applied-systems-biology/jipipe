package org.hkijena.acaq5.api.parameters;

import com.google.common.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Interfaced for a parameterized object
 */
public interface ACAQParameterCollection {
    /**
     * Gets the event bus that posts events about the parameters
     *
     * @return The event bus triggering {@link org.hkijena.acaq5.api.events.ParameterChangedEvent} and {@link org.hkijena.acaq5.api.events.ParameterStructureChangedEvent}
     */
    EventBus getEventBus();

    /**
     * Finds all parameters of the provided object
     * This includes dynamic parameters
     *
     * @param parameterHolder Parameterized object
     * @return All parameters
     */
    static Map<String, ACAQParameterAccess> getParameters(ACAQParameterCollection parameterHolder) {
        Map<String, ACAQParameterAccess> result = new HashMap<>();
        if (parameterHolder instanceof ACAQCustomParameterCollection) {
            for (Map.Entry<String, ACAQParameterAccess> entry : ((ACAQCustomParameterCollection) parameterHolder).getCustomParameters().entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        } else {
            for (Map.Entry<String, ACAQParameterAccess> entry : ACAQReflectionParameterAccess.getReflectionParameters(parameterHolder).entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }
}
