package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationOperationRegistryTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void registerAndGet_returnsOperation() {
        InstrumentationOperationRegistry registry = new InstrumentationOperationRegistry();
        InstrumentationOperation op = createOp("test_op", "Test operation");

        registry.register("test_op", op);

        assertSame(op, registry.get("test_op"));
        assertTrue(registry.has("test_op"));
    }

    @Test
    void get_unknownId_returnsNull() {
        InstrumentationOperationRegistry registry = new InstrumentationOperationRegistry();
        assertNull(registry.get("nonexistent"));
        assertFalse(registry.has("nonexistent"));
    }

    @Test
    void getIds_returnsAllRegisteredIds() {
        InstrumentationOperationRegistry registry = new InstrumentationOperationRegistry();
        registry.register("op1", createOp("op1", "One"));
        registry.register("op2", createOp("op2", "Two"));

        assertEquals(2, registry.getIds().size());
        assertTrue(registry.getIds().contains("op1"));
        assertTrue(registry.getIds().contains("op2"));
    }

    private InstrumentationOperation createOp(String id, String desc) {
        return new InstrumentationOperation() {
            @Override public String getId() { return id; }
            @Override public String getDescription() { return desc; }
            @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
                return mapper.createObjectNode();
            }
        };
    }
}
