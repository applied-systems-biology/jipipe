package org.hkijena.jipipe.api.capabilities;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityPluginRegistrationTest {

    @Test
    void capabilityServiceIsWiredIntoJIPipe() {
        JIPipe.ensureInstance(); // per AGENTS.md: never touch ij.* before this listener path
        assertNotNull(JIPipe.getCapabilities());
    }

    @Test
    void coreAiCapabilitiesAreRegistered() {
        JIPipe.ensureInstance();
        // presence, not count (plugin-aware registry rule)
        assertTrue(JIPipe.getCapabilities().find("ai:embedding").isPresent());
        assertTrue(JIPipe.getCapabilities().find("ai:mcp").isPresent());
        assertTrue(JIPipe.getCapabilities().find("ai:acp-client").isPresent());
        assertTrue(JIPipe.getCapabilities().find("acp:agent").isPresent());
        assertEquals(List.of("ai:embedding"), JIPipe.getCapabilities().find("ai:mcp").orElseThrow().getRequires());
        assertEquals(List.of("ai:mcp"), JIPipe.getCapabilities().find("ai:acp-client").orElseThrow().getRequires());
    }
}
