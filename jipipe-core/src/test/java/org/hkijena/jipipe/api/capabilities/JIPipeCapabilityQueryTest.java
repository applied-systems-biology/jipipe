package org.hkijena.jipipe.api.capabilities;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeCapabilityQueryTest {

    @Test
    void cellpose4ReportsAutoOnDemandInstallAndItsCandidates() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides("artifact:com.github.mouseland.cellpose4.cu124")
                .installHint(new JIPipeInstallHint("auto_on_demand", "4GB", null)).build());
        JIPipeCapabilityQuery q = reg.query("cellpose4");
        assertEquals(JIPipeCapabilityStatus.State.NEEDS_SETUP, q.status().state());
        assertEquals("auto_on_demand", q.install().method());
        assertEquals("4GB", q.install().size());
        assertEquals(1, q.candidates().size());
        assertFalse(q.candidates().get(0).present());
    }

    @Test
    void presentProviderAppearsInProvidedBy() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.AVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("python3")
                .alias("python3", "python").build());
        JIPipeCapabilityQuery q = reg.query("python");
        assertEquals(JIPipeCapabilityStatus.State.AVAILABLE, q.status().state());
        assertEquals(List.of("executable:python3"), q.status().providedBy());
    }

    @Test
    void intermediateSegmentDoesNotMatchAsACandidate() {
        // github is an intermediate dot-segment of the cellpose id, not the
        // last one, so the provider must not satisfy a query for it.
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides("artifact:com.github.mouseland.cellpose4.cu124")
                .installHint(new JIPipeInstallHint("auto_on_demand", "4GB", null)).build());
        JIPipeCapabilityQuery q = reg.query("github");
        assertTrue(q.candidates().isEmpty());
        assertEquals(JIPipeCapabilityStatus.State.MISSING, q.status().state());
    }

    @Test
    void partialNameDoesNotMatchAsACandidate() {
        // model is a string prefix of the model2 segment but not equal to the
        // last segment, so the provider must not satisfy a query for it.
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides("artifact:com.x.model2").build());
        JIPipeCapabilityQuery q = reg.query("model");
        assertTrue(q.candidates().isEmpty());
        assertEquals(JIPipeCapabilityStatus.State.MISSING, q.status().state());
    }
}
