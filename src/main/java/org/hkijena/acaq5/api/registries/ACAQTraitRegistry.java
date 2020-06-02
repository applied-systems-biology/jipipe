package org.hkijena.acaq5.api.registries;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.TraitRegisteredEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.traits.ACAQJavaTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains all known {@link ACAQTrait} types
 */
public class ACAQTraitRegistry implements ACAQValidatable {
    private Map<String, ACAQTraitDeclaration> registeredTraits = new HashMap<>();
    private Map<String, ACAQDependency> registeredTraitSources = new HashMap<>();
    private Set<ACAQTraitRegistrationTask> registrationTasks = new HashSet<>();
    private EventBus eventBus = new EventBus();

    /**
     * New registry
     */
    public ACAQTraitRegistry() {

    }

    /**
     * Registers a trait class
     *
     * @param id     trait id
     * @param klass  trait class
     * @param source dependency that registers the trait
     */
    public void register(String id, Class<? extends ACAQTrait> klass, ACAQDependency source) {
        register(new ACAQJavaTraitDeclaration(id, klass), source);
    }

    /**
     * Registers a trait declaration
     *
     * @param declaration trait declaration
     * @param source      dependency that registers the trait
     */
    public void register(ACAQTraitDeclaration declaration, ACAQDependency source) {
        registeredTraits.put(declaration.getId(), declaration);
        registeredTraitSources.put(declaration.getId(), source);
        eventBus.post(new TraitRegisteredEvent(declaration));
        System.out.println("Registered annotation '" + declaration.getName() + "' [" + declaration.getId() + "]");
        runRegistrationTasks();
    }

    /**
     * Schedules registration after all dependencies of the registration task are satisfied
     *
     * @param task registration task
     */
    public void scheduleRegister(ACAQTraitRegistrationTask task) {
        registrationTasks.add(task);
        runRegistrationTasks();
    }

    /**
     * Gets a registered declaration by its ID
     *
     * @param id trait id
     * @return trait declaration by id
     */
    public ACAQTraitDeclaration getDeclarationById(String id) {
        ACAQTraitDeclaration declaration = registeredTraits.getOrDefault(id, null);
        if (declaration == null) {
            throw new UserFriendlyRuntimeException(new NullPointerException("Could not find annotation type with id '" + id + "' in " +
                    String.join(", ", registeredTraits.keySet())),
                    "Unable to find an annotation type!",
                    "ACAQ5 plugin manager",
                    "A project or extension requires an annotation of type '" + id + "'. It could not be found.",
                    "Check if ACAQ5 is up-to-date and the newest version of all plugins are installed. If you know that an annotation was assigned a new ID, " +
                            "search for '" + id + "' in the JSON file and replace it with the new identifier.");
        }
        return declaration;
    }

    /**
     * Returns all registered traits
     *
     * @return all registered traits
     */
    public Map<String, ACAQTraitDeclaration> getRegisteredTraits() {
        return Collections.unmodifiableMap(registeredTraits);
    }

    /**
     * Returns true if there is a trait with given ID
     *
     * @param id trait id
     * @return true if there is a trait with given ID
     */
    public boolean hasTraitWithId(String id) {
        return registeredTraits.containsKey(id);
    }

    /**
     * Returns the source of the trait
     *
     * @param id trait id
     * @return dependency that registered the trait with id
     */
    public ACAQDependency getSourceOf(String id) {
        return registeredTraitSources.getOrDefault(id, null);
    }

    /**
     * Attempts to run registration tasks that have registered dependecies
     */
    public void runRegistrationTasks() {
        if (registrationTasks.isEmpty())
            return;
        System.out.println("There are still " + registrationTasks.size() + " unregistered annotations waiting for dependencies");
        boolean changed = true;
        while (changed) {
            changed = false;

            for (ACAQTraitRegistrationTask task : ImmutableList.copyOf(registrationTasks)) {
                if (task.canRegister()) {
                    registrationTasks.remove(task);
                    task.register();
                    changed = true;
                }
            }
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Gets the traits registered by the dependency
     *
     * @param dependency the dependency
     * @return list of traits registered by the dependency
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

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (ACAQTraitRegistrationTask task : registrationTasks) {
            report.forCategory("Unregistered annotations").forCategory(task.toString()).report(task);
        }

    }

    public static ACAQTraitRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getTraitRegistry();
    }
}
