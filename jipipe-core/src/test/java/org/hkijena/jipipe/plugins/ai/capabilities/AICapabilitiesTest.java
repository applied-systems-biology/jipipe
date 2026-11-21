package org.hkijena.jipipe.plugins.ai.capabilities;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityQuery;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AICapabilitiesTest {

    @Test
    void mcpIsBlockedWhenEmbeddingIsNotInstalled() {
        JIPipe.ensureInstance();
        JIPipeCapabilityQuery embedding = JIPipe.getCapabilities().query("ai:embedding");
        if (embedding.status().providedBy().isEmpty()) {
            // test machine has no bundled embedding model
            assertTrue(JIPipe.getCapabilities().query("ai:mcp").status().blockedBy().contains("ai:embedding"));
        }
    }
}
