package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.capabilities.JIPipeCapability;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityDomain;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityProvider;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityRegistry;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;

import java.util.Objects;

/**
 * Service component that holds the capability registry.
 * <p>
 * The registry is held here (not as a static singleton) so every consumer
 * reaches the same instance through the service lifecycle: the plugin
 * registration API, the AI settings store, and the MCP layer.
 * <p>
 * Registration is idempotent on equal definitions: re-registering the same
 * capability id or domain id with an equal definition is silently ignored,
 * while a differing definition fails loudly with an
 * {@link IllegalStateException}. Providers are never deduplicated: several
 * providers may satisfy the same capability.
 */
public class JIPipeCapabilitiesServiceComponent extends JIPipeServiceComponent {
    private final JIPipeCapabilityRegistry registry = new JIPipeCapabilityRegistry();

    public JIPipeCapabilitiesServiceComponent(JIPipeService service) {
        super(service);
    }

    public JIPipeCapabilityRegistry getRegistry() {
        return registry;
    }

    /**
     * Registers a capability domain. Re-registering the same id with an equal
     * definition is ignored; a differing definition is rejected.
     *
     * @throws IllegalStateException when the id is already registered with a
     *                                different definition
     */
    public void registerDomain(JIPipeCapabilityDomain domain) {
        Objects.requireNonNull(domain, "domain");
        JIPipeCapabilityDomain existing = findDomain(domain.getId());
        if (existing != null) {
            if (equivalentDomain(existing, domain)) {
                return;
            }
            throw new IllegalStateException("Capability domain '" + domain.getId()
                    + "' is already registered with a different definition");
        }
        registry.registerDomain(domain);
    }

    /**
     * Registers a capability definition. Re-registering the same id with an
     * equal definition is ignored; a differing definition is rejected.
     *
     * @throws IllegalStateException when the id is already registered with a
     *                                different definition
     */
    public void registerCapability(JIPipeCapability capability) {
        Objects.requireNonNull(capability, "capability");
        JIPipeCapability existing = registry.find(capability.getId()).orElse(null);
        if (existing != null) {
            if (equivalentCapability(existing, capability)) {
                return;
            }
            throw new IllegalStateException("Capability '" + capability.getId()
                    + "' is already registered with a different definition");
        }
        registry.register(capability);
    }

    /**
     * Registers a capability provider. Providers are not deduplicated.
     */
    public void registerProvider(JIPipeCapabilityProvider provider) {
        registry.registerProvider(Objects.requireNonNull(provider, "provider"));
    }

    private JIPipeCapabilityDomain findDomain(String id) {
        for (JIPipeCapabilityDomain domain : registry.getDomains()) {
            if (domain.getId().equals(id)) {
                return domain;
            }
        }
        return null;
    }

    private static boolean equivalentDomain(JIPipeCapabilityDomain a, JIPipeCapabilityDomain b) {
        return Objects.equals(a.getName(), b.getName())
                && Objects.equals(a.getDescription(), b.getDescription())
                && Objects.equals(a.getIconPath(), b.getIconPath());
    }

    private static boolean equivalentCapability(JIPipeCapability a, JIPipeCapability b) {
        return Objects.equals(a.getDomain(), b.getDomain())
                && Objects.equals(a.getName(), b.getName())
                && Objects.equals(a.getDescription(), b.getDescription())
                && Objects.equals(a.getIconPath(), b.getIconPath())
                && Objects.equals(a.getRequires(), b.getRequires());
    }
}
