package org.hkijena.acaq5.api.registries;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.AlgorithmRegisteredEvent;
import org.hkijena.acaq5.api.events.DatatypeRegisteredEvent;
import org.hkijena.acaq5.api.events.TraitRegisteredEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class ACAQAlgorithmRegistry {
    private Map<String, ACAQAlgorithmDeclaration> registeredAlgorithms = new HashMap<>();
    private Set<ACAQAlgorithmRegistrationTask> registrationTasks = new HashSet<>();
    private Map<String, ACAQDependency> registeredAlgorithmSources = new HashMap<>();
    private EventBus eventBus = new EventBus();

    public ACAQAlgorithmRegistry() {

    }

    /**
     * Schedules registration after all dependencies of the registration task are satisfied
     *
     * @param task
     */
    public void scheduleRegister(ACAQAlgorithmRegistrationTask task) {
        registrationTasks.add(task);
        runRegistrationTasks();
    }

    /**
     * Attempts to run registration tasks that have registered dependecies
     */
    public void runRegistrationTasks() {
        if (registrationTasks.isEmpty())
            return;
        System.out.println("There are still " + registrationTasks.size() + " unregistered algorithms waiting for dependencies");
        boolean changed = true;
        while (changed) {
            changed = false;

            for (ACAQAlgorithmRegistrationTask task : ImmutableList.copyOf(registrationTasks)) {
                if (task.canRegister()) {
                    registrationTasks.remove(task);
                    task.register();
                    changed = true;
                }
            }
        }
    }

    public Set<ACAQAlgorithmRegistrationTask> getScheduledRegistrationTasks() {
        return Collections.unmodifiableSet(registrationTasks);
    }

    /**
     * Registers an algorithm declaration
     *
     * @param declaration
     * @param source
     */
    public void register(ACAQAlgorithmDeclaration declaration, ACAQDependency source) {
        registeredAlgorithms.put(declaration.getId(), declaration);
        registeredAlgorithmSources.put(declaration.getId(), source);
        eventBus.post(new AlgorithmRegisteredEvent(declaration));
        System.out.println("Registered algorithm '" + declaration.getName() + "' [" + declaration.getId() + "]");
        runRegistrationTasks();
    }

    /**
     * Gets the set of all known algorithms
     *
     * @return
     */
    public Map<String, ACAQAlgorithmDeclaration> getRegisteredAlgorithms() {
        return Collections.unmodifiableMap(registeredAlgorithms);
    }

    /**
     * Returns data source algorithms that can generate the specified data type
     *
     * @param <T>
     * @param dataClass
     * @return
     */
    public <T extends ACAQData> Set<ACAQAlgorithmDeclaration> getDataSourcesFor(Class<? extends T> dataClass) {
        Set<ACAQAlgorithmDeclaration> result = new HashSet<>();
        for (ACAQAlgorithmDeclaration declaration : registeredAlgorithms.values()) {
            if (declaration.getCategory() == ACAQAlgorithmCategory.DataSource) {
                if (declaration.getOutputSlots().stream().anyMatch(slot -> slot.value() == dataClass)) {
                    result.add(declaration);
                }
            }
        }
        return result;
    }

    /**
     * Gets all algorithms of specified category
     *
     * @param category
     * @return
     */
    public Set<ACAQAlgorithmDeclaration> getAlgorithmsOfCategory(ACAQAlgorithmCategory category) {
        return registeredAlgorithms.values().stream().filter(d -> d.getCategory() == category).collect(Collectors.toSet());
    }

    /**
     * Gets a matching declaration by Id
     *
     * @param id
     * @return
     */
    public ACAQAlgorithmDeclaration getDeclarationById(String id) {
        return registeredAlgorithms.get(id);
    }

    /**
     * Returns true if the algorithm ID already exists
     *
     * @param id
     * @return
     */
    public boolean hasAlgorithmWithId(String id) {
        return registeredAlgorithms.containsKey(id);
    }

    /**
     * Returns the event bus that posts registration events
     *
     * @return
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Install registration events.
     * This method is only used internally.
     */
    public void installEvents() {
        ACAQDefaultRegistry.getInstance().getTraitRegistry().getEventBus().register(this);
        ACAQDefaultRegistry.getInstance().getDatatypeRegistry().getEventBus().register(this);
    }

    /**
     * Triggered when a trait was registered.
     * Attempts to register more algorithms
     *
     * @param event
     */
    @Subscribe
    public void onTraitRegistered(TraitRegisteredEvent event) {
        runRegistrationTasks();
    }

    /**
     * Triggered when a datatype was registered.
     * Attempts to register more algorithms.
     * @param event
     */
    @Subscribe
    public void onDatatypeRegistered(DatatypeRegisteredEvent event) {
        runRegistrationTasks();
    }

    /**
     * Returns the source of a registered algorithm
     *
     * @param algorithmId
     * @return
     */
    public ACAQDependency getSourceOf(String algorithmId) {
        return registeredAlgorithmSources.getOrDefault(algorithmId, null);
    }

    /**
     * Gets all algorithms declared by the dependency
     *
     * @param dependency
     * @return
     */
    public Set<ACAQAlgorithmDeclaration> getDeclaredBy(ACAQDependency dependency) {
        Set<ACAQAlgorithmDeclaration> result = new HashSet<>();
        for (Map.Entry<String, ACAQAlgorithmDeclaration> entry : registeredAlgorithms.entrySet()) {
            ACAQDependency source = getSourceOf(entry.getKey());
            if (source == dependency)
                result.add(entry.getValue());
        }
        return result;
    }

    public static ACAQAlgorithmRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getAlgorithmRegistry();
    }

}
