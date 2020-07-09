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

package org.hkijena.pipelinej.api.parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.pipelinej.api.events.ParameterStructureChangedEvent;
import org.hkijena.pipelinej.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.pipelinej.utils.JsonUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interfaced for a parameterized object
 */
public interface ACAQParameterCollection {
    /**
     * Gets the event bus that posts events about the parameters
     *
     * @return The event bus triggering {@link org.hkijena.pipelinej.api.events.ParameterChangedEvent} and {@link org.hkijena.pipelinej.api.events.ParameterStructureChangedEvent}
     */
    EventBus getEventBus();

    /**
     * Deserializes parameters from JSON
     *
     * @param target the target object that contains the parameters
     * @param node   the JSON node
     */
    static void deserializeParametersFromJson(ACAQParameterCollection target, JsonNode node) {
        AtomicBoolean changedStructure = new AtomicBoolean();
        changedStructure.set(true);
        target.getEventBus().register(new Object() {
            @Subscribe
            public void onParametersChanged(ParameterStructureChangedEvent event) {
                changedStructure.set(true);
            }
        });
        Set<String> loadedParameters = new HashSet<>();
        while (changedStructure.get()) {
            changedStructure.set(false);
            ACAQParameterTree parameterCollection = new ACAQParameterTree(target);
            for (ACAQParameterAccess parameterAccess : parameterCollection.getParametersByPriority()) {
                if (!parameterAccess.isPersistent())
                    continue;
                String key = parameterCollection.getUniqueKey(parameterAccess);
                if (loadedParameters.contains(key))
                    continue;
                loadedParameters.add(key);
                if (node.has(key)) {
                    Object v;
                    try {
                        v = JsonUtils.getObjectMapper().readerFor(parameterAccess.getFieldClass()).readValue(node.get(key));
                    } catch (IOException e) {
                        throw new UserFriendlyRuntimeException(e, "Could not load parameter '" + key + "'!",
                                "Load parameters from JSON", "Either the data was corrupted, or your ACAQ5 or plugin version is too new or too old.",
                                "Check the 'dependencies' section of the project file and compare the plugin versions. Try " +
                                        "to update ACAQ5. Compare the project file with a valid one. Contact the ACAQ5 or plugin " +
                                        "authors if you cannot resolve the issue by yourself.");
                    }
                    parameterAccess.set(v);

                    // Stop loading here to prevent already traversed parameters from being not loaded
                    if (changedStructure.get())
                        break;
                }
            }
        }
    }
}
