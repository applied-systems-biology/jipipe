package org.hkijena.acaq5.api.registries;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.AlgorithmRegisteredEvent;
import org.hkijena.acaq5.api.events.DatatypeRegisteredEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class ACAQAlgorithmRegistry implements ACAQValidatable {
    private Map<String, ACAQAlgorithmDeclaration> registeredAlgorithms = new HashMap<>();
    private Set<ACAQAlgorithmRegistrationTask> registrationTasks = new HashSet<>();
    private Map<String, ACAQDependency> registeredAlgorithmSources = new HashMap<>();
    private boolean stateChanged;
    private boolean isRunning;
    private EventBus eventBus = new EventBus();

    /**
     * Creates a new registry
     */
    public ACAQAlgorithmRegistry() {

    }

    /**
     * @return Singleton instance
     */
    public static ACAQAlgorithmRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getAlgorithmRegistry();
    }

    /**
     * Schedules registration after all dependencies of the registration task are satisfied
     *
     * @param task A registration task
     */
    public void scheduleRegister(ACAQAlgorithmRegistrationTask task) {
        registrationTasks.add(task);
        runRegistrationTasks();
    }

    /**
     * Attempts to run registration tasks that have registered dependencies
     */
    public void runRegistrationTasks() {
        if (registrationTasks.isEmpty())
            return;
        stateChanged = true;
        run();
    }

    private void run() {
        if (isRunning)
            return;
        isRunning = true;
        while (stateChanged) {
            stateChanged = false;
            for (ACAQAlgorithmRegistrationTask task : ImmutableList.copyOf(registrationTasks)) {
                if (task.canRegister()) {
                    registrationTasks.remove(task);
                    task.register();
                    stateChanged = true;
                }
            }
        }
        isRunning = false;
    }

    /**
     * @return The list of current registration tasks
     */
    public Set<ACAQAlgorithmRegistrationTask> getScheduledRegistrationTasks() {
        return Collections.unmodifiableSet(registrationTasks);
    }

    /**
     * Registers an algorithm declaration
     *
     * @param declaration The algorithm declaration
     * @param source      The dependency that registers the declaration
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
     * @return Map from algorithm ID to algorithm declaration
     */
    public Map<String, ACAQAlgorithmDeclaration> getRegisteredAlgorithms() {
        return Collections.unmodifiableMap(registeredAlgorithms);
    }

    /**
     * Returns data source algorithms that can generate the specified data type
     *
     * @param <T>       The data class
     * @param dataClass The data class
     * @return Available datasource algorithms that generate the data
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
     * @param category The category
     * @return Algorithms within the specified category
     */
    public Set<ACAQAlgorithmDeclaration> getAlgorithmsOfCategory(ACAQAlgorithmCategory category) {
        return registeredAlgorithms.values().stream().filter(d -> d.getCategory() == category).collect(Collectors.toSet());
    }

    /**
     * Gets a matching declaration by Id
     *
     * @param id The declaration ID. Must exist.
     * @return The declaration
     */
    public ACAQAlgorithmDeclaration getDeclarationById(String id) {
        ACAQAlgorithmDeclaration declaration = registeredAlgorithms.getOrDefault(id, null);
        if (declaration == null) {
            throw new UserFriendlyRuntimeException(new NullPointerException("Could not find algorithm declaration with id '" + id + "' in " +
                    String.join(", ", registeredAlgorithms.keySet())),
                    "Unable to find an algorithm type!",
                    "ACAQ5 plugin manager",
                    "A project or extension requires an algorithm of type '" + id + "'. It could not be found.",
                    "Check if ACAQ5 is up-to-date and the newest version of all plugins are installed. If you know that an algorithm was assigned a new ID, " +
                            "search for '" + id + "' in the JSON file and replace it with the new identifier.");
        }
        return declaration;
    }

    /**
     * Returns true if the algorithm ID already exists
     *
     * @param id The declaration ID
     * @return If true, the ID exists
     */
    public boolean hasAlgorithmWithId(String id) {
        return registeredAlgorithms.containsKey(id);
    }

    /**
     * Returns the event bus that posts registration events
     *
     * @return Event bus instance
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Install registration events.
     * This method is only used internally.
     */
    public void installEvents() {
        ACAQDefaultRegistry.getInstance().getDatatypeRegistry().getEventBus().register(this);
    }

    /**
     * Triggered when a datatype was registered.
     * Attempts to register more algorithms.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onDatatypeRegistered(DatatypeRegisteredEvent event) {
        runRegistrationTasks();
    }

    /**
     * Returns the source of a registered algorithm
     *
     * @param algorithmId The algorithm declaration ID
     * @return The dependency that registered the algorithm
     */
    public ACAQDependency getSourceOf(String algorithmId) {
        return registeredAlgorithmSources.getOrDefault(algorithmId, null);
    }

    /**
     * Gets all algorithms declared by the dependency
     *
     * @param dependency The dependency
     * @return All algorithms that were registered by this dependency
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

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (ACAQAlgorithmRegistrationTask task : registrationTasks) {
            report.forCategory("Unregistered algorithms").forCategory(task.toString()).report(task);
        }
    }

}
