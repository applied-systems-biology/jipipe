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

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the JSON contract of {@link MCPPermissionStates}: the settings parameter must
 * serialize to the flat {@code operation-id -> state} object shape of the pre-table
 * permission map (Task 7), so profiles written before the permission table keep loading.
 * The serialized form is pinned byte-exact as a compat regression guard.
 */
public class MCPPermissionStatesTest {

    @Test
    public void serializesToFlatLegacyMapShape() throws Exception {
        MCPPermissionStates states = new MCPPermissionStates();
        states.set("query_graph", "Allow");
        states.set("run_pipeline", "Ask");
        states.set("set_parameter", "Deny");
        assertEquals("{\"query_graph\":\"Allow\",\"run_pipeline\":\"Ask\",\"set_parameter\":\"Deny\"}",
                JsonUtils.getObjectMapper().writeValueAsString(states));
    }

    @Test
    public void deserializesLegacyFlatMapJson() throws Exception {
        MCPPermissionStates states = JsonUtils.getObjectMapper().readValue(
                "{\"query_graph\":\"Ask\",\"run_pipeline\":\"Allow\",\"set_parameter\":\"Deny\"}",
                MCPPermissionStates.class);
        assertEquals("Ask", states.get("query_graph"));
        assertEquals("Allow", states.get("run_pipeline"));
        assertEquals("Deny", states.get("set_parameter"));
        assertEquals(3, states.asMap().size());
    }

    @Test
    public void roundTripsThroughJson() throws Exception {
        MCPPermissionStates states = new MCPPermissionStates();
        states.set("query_graph", "Allow");
        states.set("run_pipeline", "Ask");
        String json = JsonUtils.getObjectMapper().writeValueAsString(states);
        MCPPermissionStates read = JsonUtils.getObjectMapper().readValue(json, MCPPermissionStates.class);
        assertEquals(states.asMap(), read.asMap());
        assertEquals(json, JsonUtils.getObjectMapper().writeValueAsString(read));
    }
}
