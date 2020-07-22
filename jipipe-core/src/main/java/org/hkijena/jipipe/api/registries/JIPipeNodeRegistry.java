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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeNodeCategory;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.events.DatatypeRegisteredEvent;
import org.hkijena.jipipe.api.events.NodeInfoRegisteredEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class JIPipeNodeRegistry implements JIPipeValidatable {
    private Map<String, JIPipeNodeInfo> registeredNodeInfos = new HashMap<>();
    private Set<JIPipeNodeRegistrationTask> registrationTasks = new HashSet<>();
    private Map<String, JIPipeDependency> registeredNodeInfoSources = new HashMap<>();
    private BiMap<Class<? extends JIPipeNodeTypeCategory>, JIPipeNodeTypeCategory> registeredCategories = HashBiMap.create();
    private boolean stateChanged;
    private boolean isRunning;
    private EventBus eventBus = new EventBus();

    /**
     * Creates a new registry
     */
    public JIPipeNodeRegistry() {

    }

    /**
     * Schedules registration after all dependencies of the registration task are satisfied
     *
     * @param task A registration task
     */
    public void scheduleRegister(JIPipeNodeRegistrationTask task) {
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
            for (JIPipeNodeRegistrationTask task : ImmutableList.copyOf(registrationTasks)) {
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
    public Set<JIPipeNodeRegistrationTask> getScheduledRegistrationTasks() {
        return Collections.unmodifiableSet(registrationTasks);
    }

    /**
     * Registers an algorithm info
     *
     * @param info   The algorithm info
     * @param source The dependency that registers the info
     */
    public void register(JIPipeNodeInfo info, JIPipeDependency source) {
        registeredNodeInfos.put(info.getId(), info);
        registeredNodeInfoSources.put(info.getId(), source);
        eventBus.post(new NodeInfoRegisteredEvent(info));
        System.out.println("Registered algorithm '" + info.getName() + "' [" + info.getId() + "]");
        runRegistrationTasks();
    }

    /**
     * Registers a category
     * @param category the category instance
     */
    public void registerCategory(JIPipeNodeTypeCategory category) {
        registeredCategories.put(category.getClass(), category);
    }

    /**
     * Gets the set of all known algorithms
     *
     * @return Map from algorithm ID to algorithm info
     */
    public Map<String, JIPipeNodeInfo> getRegisteredNodeInfos() {
        return Collections.unmodifiableMap(registeredNodeInfos);
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
        for (JIPipeNodeInfo info : registeredNodeInfos.values()) {
            if (info.getCategory() == JIPipeNodeCategory.DataSource) {
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
    public Set<JIPipeNodeInfo> getNodesOfCategory(JIPipeNodeCategory category) {
        return registeredNodeInfos.values().stream().filter(d -> d.getCategory() == category).collect(Collectors.toSet());
    }

    /**
     * Gets a matching info by Id
     *
     * @param id The info ID. Must exist.
     * @return The info
     */
    public JIPipeNodeInfo getInfoById(String id) {
        JIPipeNodeInfo info = registeredNodeInfos.getOrDefault(id, null);
        if (info == null) {
            throw new UserFriendlyRuntimeException(new NullPointerException("Could not find algorithm info with id '" + id + "' in " +
                    String.join(", ", registeredNodeInfos.keySet())),
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
    public boolean hasNodeInfoWithId(String id) {
        return registeredNodeInfos.containsKey(id);
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
        return registeredNodeInfoSources.getOrDefault(algorithmId, null);
    }

    /**
     * Gets all algorithms declared by the dependency
     *
     * @param dependency The dependency
     * @return All algorithms that were registered by this dependency
     */
    public Set<JIPipeNodeInfo> getDeclaredBy(JIPipeDependency dependency) {
        Set<JIPipeNodeInfo> result = new HashSet<>();
        for (Map.Entry<String, JIPipeNodeInfo> entry : registeredNodeInfos.entrySet()) {
            JIPipeDependency source = getSourceOf(entry.getKey());
            if (source == dependency)
                result.add(entry.getValue());
        }
        return result;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (JIPipeNodeRegistrationTask task : registrationTasks) {
            report.forCategory("Unregistered algorithms").forCategory(task.toString()).report(task);
        }
    }

    public BiMap<Class<? extends JIPipeNodeTypeCategory>, JIPipeNodeTypeCategory> getRegisteredCategories() {
        return ImmutableBiMap.copyOf(registeredCategories);
    }

    /**
     * @return Singleton instance
     */
    public static JIPipeNodeRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getNodeRegistry();
    }

}
