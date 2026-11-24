package org.hkijena.jipipe.api.automation.operations;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityOperationsTest {

    @Test
    void statusOperationSchemaRequiresCapability() {
        CapabilityOperations.GetStatus op = new CapabilityOperations.GetStatus();
        assertEquals("get_capability_status", op.getId());
        assertTrue(op.isReadOnly());
        assertEquals("get_capability_status", op.getId());
        assertTrue(op.getInputSchema().toString().contains("\"capability\""));
    }

    @Test
    void installOperationIsAsync() {
        CapabilityOperations.Install op = new CapabilityOperations.Install();
        assertInstanceOf(org.hkijena.jipipe.api.automation.AsyncAutomationOperation.class, op);
        assertFalse(op.isReadOnly());
    }
}
