package org.hkijena.acaq5.api.registries;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.events.TraitRegisteredEvent;
import org.hkijena.acaq5.api.traits.ACAQDefaultTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains all known {@link ACAQTrait} types
 */
public class ACAQTraitRegistry {
    private Map<String, ACAQTraitDeclaration> registeredTraits = new HashMap<>();
    private EventBus eventBus = new EventBus();

    public ACAQTraitRegistry() {

    }

    public void register(Class<? extends ACAQTrait> klass) {
        register(new ACAQDefaultTraitDeclaration(klass));
    }

    public void register(ACAQTraitDeclaration declaration) {
        registeredTraits.put(declaration.getId(), declaration);
        eventBus.post(new TraitRegisteredEvent(declaration));
    }

    public ACAQTraitDeclaration getDefaultDeclarationFor(Class<? extends ACAQTrait> klass) {
        return Objects.requireNonNull(registeredTraits.get(ACAQDefaultTraitDeclaration.getDeclarationIdOf(klass)));
    }

    public boolean hasDefaultDeclarationFor(Class<? extends ACAQTrait> klass) {
        return registeredTraits.containsKey(ACAQDefaultTraitDeclaration.getDeclarationIdOf(klass));
    }

    public ACAQTraitDeclaration getDeclarationById(String id) {
        return Objects.requireNonNull(registeredTraits.get(id));
    }

    public Map<String, ACAQTraitDeclaration> getRegisteredTraits() {
        return Collections.unmodifiableMap(registeredTraits);
    }

    public boolean hasTraitWithId(String id) {
        return registeredTraits.containsKey(id);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public static ACAQTraitRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getTraitRegistry();
    }
}
