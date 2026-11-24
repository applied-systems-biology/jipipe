package org.hkijena.jipipe.api.capabilities;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeCapabilityRequirementPlanTest {

    @Test
    void expandingAcpFindsMcpThenEmbedding() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("embedding", "ai", "Embedding", "", null, List.of()));
        reg.register(new JIPipeCapability("mcp", "ai", "MCP", "", null, List.of("embedding")));
        reg.register(new JIPipeCapability("acp", "ai", "ACP", "", null, List.of("mcp", "embedding")));
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides("artifact:embedding").build());
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.SERVICE).provides("service:mcp").build());
        JIPipeCapabilityRequirementPlan plan = reg.planInstall("acp");
        assertEquals(List.of("embedding", "mcp"), plan.installOrder());
        assertEquals(List.of("acp", "mcp"), plan.rows().stream()
                .map(r -> r.reason()).distinct().sorted().toList());
    }

    @Test
    void missingProviderYieldsEmptyOptions() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("x", "d", "X", "", null, List.of()));
        JIPipeCapabilityRequirementPlan plan = reg.planInstall("x");
        assertEquals(1, plan.rows().size());
        assertTrue(plan.rows().get(0).options().isEmpty());
        assertNull(plan.rows().get(0).preselectedProviderId());
    }
}
