package org.hkijena.acaq5.api.registries;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.events.TraitRegisteredEvent;
import org.hkijena.acaq5.api.traits.ACAQJavaTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.*;

/**
 * Contains all known {@link ACAQTrait} types
 */
public class ACAQTraitRegistry {
    private Map<String, ACAQTraitDeclaration> registeredTraits = new HashMap<>();
    private Map<String, ACAQDependency> registeredTraitSources = new HashMap<>();
    private EventBus eventBus = new EventBus();

    public ACAQTraitRegistry() {

    }

    /**
     * Registers a trait class
     *
     * @param klass
     * @param source
     */
    public void register(String id, Class<? extends ACAQTrait> klass, ACAQDependency source) {
        register(new ACAQJavaTraitDeclaration(id, klass), source);
    }

    /**
     * Registers a trait declaration
     *
     * @param declaration
     * @param source
     */
    public void register(ACAQTraitDeclaration declaration, ACAQDependency source) {
        registeredTraits.put(declaration.getId(), declaration);
        registeredTraitSources.put(declaration.getId(), source);
        eventBus.post(new TraitRegisteredEvent(declaration));
    }

    /**
     * Gets a registered declaration by its ID
     *
     * @param id
     * @return
     */
    public ACAQTraitDeclaration getDeclarationById(String id) {
        return Objects.requireNonNull(registeredTraits.get(id));
    }

    /**
     * Returns all registered traits
     *
     * @return
     */
    public Map<String, ACAQTraitDeclaration> getRegisteredTraits() {
        return Collections.unmodifiableMap(registeredTraits);
    }

    /**
     * Returns true if there is a trait with given ID
     *
     * @param id
     * @return
     */
    public boolean hasTraitWithId(String id) {
        return registeredTraits.containsKey(id);
    }

    /**
     * Returns the source of the trait
     *
     * @param id
     * @return
     */
    public ACAQDependency getSourceOf(String id) {
        return registeredTraitSources.getOrDefault(id, null);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Gets the traits registered by the dependency
     *
     * @param dependency
     * @return
     */
    public Set<ACAQTraitDeclaration> getDeclaredBy(ACAQDependency dependency) {
        Set<ACAQTraitDeclaration> result = new HashSet<>();
        for (Map.Entry<String, ACAQTraitDeclaration> entry : registeredTraits.entrySet()) {
            ACAQDependency source = getSourceOf(entry.getKey());
            if (source == dependency)
                result.add(entry.getValue());
        }
        return result;
    }

    public static ACAQTraitRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getTraitRegistry();
    }
}
