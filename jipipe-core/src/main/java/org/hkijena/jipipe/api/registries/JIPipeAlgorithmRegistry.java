/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.registries;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.events.AlgorithmRegisteredEvent;
import org.hkijena.jipipe.api.events.DatatypeRegisteredEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class JIPipeAlgorithmRegistry implements JIPipeValidatable {
    private Map<String, JIPipeNodeInfo> registeredAlgorithms = new HashMap<>();
    private Set<JIPipeAlgorithmRegistrationTask> registrationTasks = new HashSet<>();
    private Map<String, JIPipeDependency> registeredAlgorithmSources = new HashMap<>();
    private boolean stateChanged;
    private boolean isRunning;
    private EventBus eventBus = new EventBus();

    /**
     * Creates a new registry
     */
    public JIPipeAlgorithmRegistry() {

    }

    /**
     * Schedules registration after all dependencies of the registration task are satisfied
     *
     * @param task A registration task
     */
    public void scheduleRegister(JIPipeAlgorithmRegistrationTask task) {
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
            for (JIPipeAlgorithmRegistrationTask task : ImmutableList.copyOf(registrationTasks)) {
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
    public Set<JIPipeAlgorithmRegistrationTask> getScheduledRegistrationTasks() {
        return Collections.unmodifiableSet(registrationTasks);
    }

    /**
     * Registers an algorithm info
     *
     * @param info The algorithm info
     * @param source      The dependency that registers the info
     */
    public void register(JIPipeNodeInfo info, JIPipeDependency source) {
        registeredAlgorithms.put(info.getId(), info);
        registeredAlgorithmSources.put(info.getId(), source);
        eventBus.post(new AlgorithmRegisteredEvent(info));
        System.out.println("Registered algorithm '" + info.getName() + "' [" + info.getId() + "]");
        runRegistrationTasks();
    }

    /**
     * Gets the set of all known algorithms
     *
     * @return Map from algorithm ID to algorithm info
     */
    public Map<String, JIPipeNodeInfo> getRegisteredAlgorithms() {
        return Collections.unmodifiableMap(registeredAlgorithms);
    }

    /**
     * Returns data source algorithms that can generate the specified data type
     *
     * @param <T>       The data class
     * @param dataClass The data class
     * @return Available datasource algorithms that generate the data
     */
    public <T extends JIPipeData> Set<JIPipeNodeInfo> getDataSourcesFor(Class<? extends T> dataClass) {
        Set<JIPipeNodeInfo> result = new HashSet<>();
        for (JIPipeNodeInfo info : registeredAlgorithms.values()) {
            if (info.getCategory() == JIPipeAlgorithmCategory.DataSource) {
                if (info.getOutputSlots().stream().anyMatch(slot -> slot.value() == dataClass)) {
                    result.add(info);
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
    public Set<JIPipeNodeInfo> getAlgorithmsOfCategory(JIPipeAlgorithmCategory category) {
        return registeredAlgorithms.values().stream().filter(d -> d.getCategory() == category).collect(Collectors.toSet());
    }

    /**
     * Gets a matching info by Id
     *
     * @param id The info ID. Must exist.
     * @return The info
     */
    public JIPipeNodeInfo getInfoById(String id) {
        JIPipeNodeInfo info = registeredAlgorithms.getOrDefault(id, null);
        if (info == null) {
            throw new UserFriendlyRuntimeException(new NullPointerException("Could not find algorithm info with id '" + id + "' in " +
                    String.join(", ", registeredAlgorithms.keySet())),
                    "Unable to find an algorithm type!",
                    "JIPipe plugin manager",
                    "A project or extension requires an algorithm of type '" + id + "'. It could not be found.",
                    "Check if JIPipe is up-to-date and the newest version of all plugins are installed. If you know that an algorithm was assigned a new ID, " +
                            "search for '" + id + "' in the JSON file and replace it with the new identifier.");
        }
        return info;
    }

    /**
     * Returns true if the algorithm ID already exists
     *
     * @param id The info ID
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
        JIPipeDefaultRegistry.getInstance().getDatatypeRegistry().getEventBus().register(this);
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
     * @param algorithmId The algorithm info ID
     * @return The dependency that registered the algorithm
     */
    public JIPipeDependency getSourceOf(String algorithmId) {
        return registeredAlgorithmSources.getOrDefault(algorithmId, null);
    }

    /**
     * Gets all algorithms declared by the dependency
     *
     * @param dependency The dependency
     * @return All algorithms that were registered by this dependency
     */
    public Set<JIPipeNodeInfo> getDeclaredBy(JIPipeDependency dependency) {
        Set<JIPipeNodeInfo> result = new HashSet<>();
        for (Map.Entry<String, JIPipeNodeInfo> entry : registeredAlgorithms.entrySet()) {
            JIPipeDependency source = getSourceOf(entry.getKey());
            if (source == dependency)
                result.add(entry.getValue());
        }
        return result;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (JIPipeAlgorithmRegistrationTask task : registrationTasks) {
            report.forCategory("Unregistered algorithms").forCategory(task.toString()).report(task);
        }
    }

    /**
     * @return Singleton instance
     */
    public static JIPipeAlgorithmRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getAlgorithmRegistry();
    }

}
