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
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.AVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("python").build());
        reg.registerProvider(present("artifact:system:python3.v312"));
        reg.registerProvider(JIPipeCapabilityProvider.builder(() -> JIPipeCapabilityStatus.State.UNAVAILABLE)
                .kind(JIPipeProviderKind.EXECUTABLE).provides("python3").build());
        assertEquals(JIPipeCapabilityStatus.State.AVAILABLE, reg.getStatus("python").state(),
                "python3.v312 satisfies python via the chain");
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
