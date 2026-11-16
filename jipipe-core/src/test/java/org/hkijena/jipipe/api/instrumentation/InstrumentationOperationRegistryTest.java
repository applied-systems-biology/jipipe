package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationOperationRegistryTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void registerAndGet_returnsOperation() {
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        AutomationOperation op = createOp("test_op", "Test operation");

        registry.register("test_op", op);

        assertSame(op, registry.get("test_op"));
        assertTrue(registry.has("test_op"));
    }

    @Test
    void get_unknownId_returnsNull() {
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        assertNull(registry.get("nonexistent"));
        assertFalse(registry.has("nonexistent"));
    }

    @Test
    void getIds_returnsAllRegisteredIds() {
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        registry.register("op1", createOp("op1", "One"));
        registry.register("op2", createOp("op2", "Two"));

        assertEquals(2, registry.getIds().size());
        assertTrue(registry.getIds().contains("op1"));
        assertTrue(registry.getIds().contains("op2"));
    }

    private AutomationOperation createOp(String id, String desc) {
        return new AutomationOperation() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getDescription() {
                return desc;
            }

            @Override
            public JsonNode getInputSchema() {
                return AutomationSchemaUtils.emptyObjectSchema();
            }

            @Override
            public JsonNode execute(AutomationContext ctx, JsonNode params) {
                return mapper.createObjectNode();
            }
        };
    }
}
