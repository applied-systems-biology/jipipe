package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.traits.ACAQDefaultTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all known {@link ACAQTrait} types
 */
public class ACAQTraitRegistry {
    private Map<String, ACAQTraitDeclaration> registeredTraits = new HashMap<>();

    public ACAQTraitRegistry() {

    }

    public void register(Class<? extends ACAQTrait> klass) {
        register(new ACAQDefaultTraitDeclaration(klass));
    }

    public void register(ACAQTraitDeclaration declaration) {
        registeredTraits.put(declaration.getId(), declaration);
    }

    public ACAQTraitDeclaration getDefaultDeclarationFor(Class<? extends ACAQTrait> klass) {
        return registeredTraits.getOrDefault(ACAQDefaultTraitDeclaration.getDeclarationIdOf(klass), null);
    }

    public ACAQTraitDeclaration getDeclarationById(String id) {
        return registeredTraits.get(id);
    }

    public static ACAQTraitRegistry getInstance() {
        return ACAQRegistryService.getInstance().getTraitRegistry();
    }
}
