/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.automation.settings;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The user-chosen {@link org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState}
 * per automation operation id, stored as the {@code permissions} parameter of
 * {@link MCPApplicationSettings}.
 * <p>
 * The JSON form is a flat object mapping operation id to state name, identical to the
 * pre-table map shape, so profiles written before the permission table keep loading.
 */
public class MCPPermissionStates {

    private final Map<String, String> states = new LinkedHashMap<>();

    public MCPPermissionStates() {
    }

    /**
     * Copy constructor used by the settings UI: an edit writes a fresh instance through
     * the parameter access, which fires the parameter change events.
     */
    public MCPPermissionStates(MCPPermissionStates other) {
        states.putAll(other.states);
    }

    /**
     * The stored states (operation id to state name). Live view; edits fire no events,
     * so the settings UI writes a copy via the parameter access instead.
     */
    @JsonAnyGetter
    public Map<String, String> getStates() {
        return states;
    }

    @JsonAnySetter
    public void setState(String operationId, String state) {
        if (operationId != null && state != null) {
            states.put(operationId, state);
        }
    }

    /**
     * Returns the state stored for one operation, or null when unset (the category
     * default applies).
     */
    public String get(String operationId) {
        return states.get(operationId);
    }

    /**
     * Stores or clears (null) the state of one operation.
     */
    public void set(String operationId, String state) {
        if (state == null) {
            states.remove(operationId);
        } else {
            states.put(operationId, state);
        }
    }

    /**
     * An unmodifiable snapshot of the stored states.
     */
    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(states));
    }
}
