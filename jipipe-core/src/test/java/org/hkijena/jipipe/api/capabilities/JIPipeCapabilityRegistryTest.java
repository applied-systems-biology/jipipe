package org.hkijena.jipipe.api.capabilities;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JIPipeCapabilityRegistryTest {

    private static JIPipeCapabilityProvider present(String... provides) {
        return JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.AVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides(provides).build();
    }
    private static JIPipeCapabilityProvider absent(String... provides) {
        return JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides(provides).build();
    }

    @Test
    void derivesLevelsByStrippingQualifiersAndKindPrefix() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        // No provider provides "python" directly. The qualified artifact can
        // only satisfy "python" by stripping the qualifier segment and the
        // kind prefix, and then following the alias python3 -> python.
        reg.registerProvider(present("artifact:python3.v312"));
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("python3")
                .alias("python3", "python").build());
        assertEquals(JIPipeCapabilityStatus.State.AVAILABLE, reg.getStatus("python").state(),
                "python3.v312 satisfies python via the chain");
    }

    @Test
    void aliasEdgesAreConsultedAcrossProviders() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        // A provides python3; B declares the family alias. Querying python
        // must resolve via A, even though the alias edge lives on B.
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.AVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("python3").build());
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("r")
                .alias("python3", "python").build());
        assertEquals(JIPipeCapabilityStatus.State.AVAILABLE, reg.getStatus("python").state(),
                "python3 satisfies python via the alias declared on another provider");
    }

    @Test
    void blockedByIsDeduplicatedAcrossRequirements() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("shared", "d", "S", "", null, List.of()));
        reg.register(new JIPipeCapability("mcp1", "d", "M1", "", null, List.of("shared")));
        reg.register(new JIPipeCapability("mcp2", "d", "M2", "", null, List.of("shared")));
        reg.register(new JIPipeCapability("top", "d", "T", "", null, List.of("mcp1", "mcp2")));
        assertEquals(List.of("shared"), reg.getStatus("top").blockedBy(),
                "two requirements with the same deepest missing capability must not duplicate it");
    }

    @Test
    void cyclicRequirementsAreRejectedAtRegistration() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("a", "d", "A", "", null, List.of("b")));
        assertThrows(IllegalArgumentException.class,
                () -> reg.register(new JIPipeCapability("b", "d", "B", "", null, List.of("a"))));
    }

    @Test
    void blockedReportsTheWholeChain() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        reg.register(new JIPipeCapability("embedding", "d", "E", "", null, List.of()));
        reg.register(new JIPipeCapability("mcp", "d", "M", "", null, List.of("embedding")));
        reg.register(new JIPipeCapability("acp", "d", "A", "", null, List.of("mcp")));
        assertEquals(List.of("embedding"), reg.getStatus("acp").blockedBy());
    }

    @Test
    void providerVanishesBetweenQueries() {
        JIPipeCapabilityRegistry reg = new JIPipeCapabilityRegistry();
        boolean[] installed = {true};
        reg.registerProvider(JIPipeCapabilityProvider.builder(() ->
                installed[0] ? JIPipeCapabilityStatus.State.AVAILABLE : JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.ARTIFACT).provides("artifact:model").build());
        assertEquals(JIPipeCapabilityStatus.State.AVAILABLE, reg.getStatus("model").state());
        installed[0] = false;
        assertEquals(JIPipeCapabilityStatus.State.MISSING, reg.getStatus("model").state());
    }
}
