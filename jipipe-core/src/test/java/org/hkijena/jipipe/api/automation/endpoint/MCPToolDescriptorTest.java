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

package org.hkijena.jipipe.api.automation.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.AutomationToolScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the tool-scope contract of {@link MCPToolDescriptor}: the descriptor carries the
 * scope declared by its operation, serializes it as "tool-scope" (always emitted), parses
 * an absent field as {@link AutomationToolScope#PROJECT_BOUND} (the legacy-registration
 * contract), and tolerates unknown fields.
 */
public class MCPToolDescriptorTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static AutomationOperation stubOperation(AutomationToolScope scope) {
        return new AutomationOperation() {
            @Override
            public String getId() {
                return "test";
            }

            @Override
            public String getDescription() {
                return "test";
            }

            @Override
            public JsonNode getInputSchema() {
                return AutomationSchemaUtils.parseJson("{\"type\":\"object\"}");
            }

            @Override
            public JsonNode execute(AutomationContext ctx, JsonNode params) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AutomationToolScope getToolScope() {
                return scope;
            }
        };
    }

    @Test
    public void scopeDefaultsToProjectBound() {
        assertEquals(AutomationToolScope.PROJECT_BOUND, new MCPToolDescriptor().getToolScope());
    }

    @Test
    public void ofCopiesTheOperationScope() {
        MCPToolDescriptor descriptor = MCPToolDescriptor.of(stubOperation(AutomationToolScope.INSTANCE_GLOBAL));
        assertEquals(AutomationToolScope.INSTANCE_GLOBAL, descriptor.getToolScope());
    }

    @Test
    public void jsonRoundTripPreservesScope() throws Exception {
        MCPToolDescriptor descriptor = MCPToolDescriptor.of(
                stubOperation(AutomationToolScope.INSTANCE_GLOBAL_AGGREGATED));
        JsonNode json = mapper.readTree(mapper.writeValueAsString(descriptor));
        assertEquals("INSTANCE_GLOBAL_AGGREGATED", json.path("tool-scope").asText());
        MCPToolDescriptor parsed = mapper.treeToValue(json, MCPToolDescriptor.class);
        assertEquals(AutomationToolScope.INSTANCE_GLOBAL_AGGREGATED, parsed.getToolScope());
    }

    @Test
    public void absentFieldParsesAsProjectBound() throws Exception {
        MCPToolDescriptor parsed = mapper.readValue(
                "{\"id\":\"test\",\"description\":\"test\",\"read-only\":true}", MCPToolDescriptor.class);
        assertEquals(AutomationToolScope.PROJECT_BOUND, parsed.getToolScope());
    }

    @Test
    public void unknownFieldsAreTolerated() throws Exception {
        MCPToolDescriptor parsed = mapper.readValue(
                "{\"id\":\"test\",\"future-field\":42,\"tool-scope\":\"NOT_A_TOOL\"}", MCPToolDescriptor.class);
        assertEquals(AutomationToolScope.NOT_A_TOOL, parsed.getToolScope());
    }
}
