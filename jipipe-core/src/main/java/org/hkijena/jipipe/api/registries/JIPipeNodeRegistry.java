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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class JIPipeNodeRegistry implements JIPipeValidatable {
    private Map<String, JIPipeNodeInfo> registeredNodeInfos = new HashMap<>();
    private Multimap<Class<? extends JIPipeGraphNode>, JIPipeNodeInfo> registeredNodeClasses = HashMultimap.create();
    private Set<JIPipeNodeRegistrationTask> registrationTasks = new HashSet<>();
    private Map<String, JIPipeDependency> registeredNodeInfoSources = new HashMap<>();
    private BiMap<String, JIPipeNodeTypeCategory> registeredCategories = HashBiMap.create();
    private Map<JIPipeNodeInfo, URL> iconURLs = new HashMap<>();
    private Map<JIPipeNodeInfo, ImageIcon> iconInstances = new HashMap<>();
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
        registeredNodeClasses.put(info.getInstanceClass(), info);
        registeredNodeInfoSources.put(info.getId(), source);
        eventBus.post(new JIPipe.NodeInfoRegisteredEvent(info));
        JIPipe.getInstance().getLogService().info("Registered algorithm '" + info.getName() + "' [" + info.getId() + "]");
        runRegistrationTasks();
    }

    public JIPipeNodeTypeCategory getCategory(String id) {
        return registeredCategories.getOrDefault(id, null);
    }

    /**
     * Registers a category
     *
     * @param category the category instance
     */
    public void registerCategory(JIPipeNodeTypeCategory category) {
        registeredCategories.put(category.getId(), category);
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
            if (info.getCategory() instanceof DataSourceNodeTypeCategory) {
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
    public Set<JIPipeNodeInfo> getNodesOfCategory(JIPipeNodeTypeCategory category) {
        return registeredNodeInfos.values().stream().filter(d -> Objects.equals(d.getCategory().getId(), category.getId())).collect(Collectors.toSet());
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
        JIPipe.getInstance().getDatatypeRegistry().getEventBus().register(this);
    }

    /**
     * Triggered when a datatype was registered.
     * Attempts to register more algorithms.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onDatatypeRegistered(JIPipe.DatatypeRegisteredEvent event) {
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

    public ImmutableBiMap<String, JIPipeNodeTypeCategory> getRegisteredCategories() {
        return ImmutableBiMap.copyOf(registeredCategories);
    }

    /**
     * Registers a custom icon for a trait
     *
     * @param info         the trait type
     * @param resourcePath icon url
     */
    public void registerIcon(JIPipeNodeInfo info, URL resourcePath) {
        iconURLs.put(info, resourcePath);
        iconInstances.put(info, new ImageIcon(resourcePath));
    }

    /**
     * Returns the icon resource path URL for a trait
     *
     * @param klass trait type
     * @return icon url
     */
    public URL getIconURLFor(JIPipeNodeInfo klass) {
        return iconURLs.getOrDefault(klass, ResourceUtils.getPluginResource("icons/actions/configure.png"));
    }

    /**
     * Returns the icon for a trait
     *
     * @param info trait type
     * @return icon instance
     */
    public ImageIcon getIconFor(JIPipeNodeInfo info) {
        ImageIcon icon = iconInstances.getOrDefault(info, null);
        if (icon == null) {
            ImageIcon defaultIcon;
            if (info.getCategory() instanceof DataSourceNodeTypeCategory) {
                if (!info.getOutputSlots().isEmpty()) {
                    defaultIcon = JIPipe.getDataTypes().getIconFor(info.getOutputSlots().get(0).value());
                } else {
                    defaultIcon = UIUtils.getIconFromResources("actions/configure.png");
                }
            } else {
                defaultIcon = UIUtils.getIconFromResources("actions/configure.png");
            }
            iconInstances.put(info, defaultIcon);
            icon = defaultIcon;
        }
        return icon;
    }

    /**
     * Returns all node infos that create a node of the specified class
     *
     * @param klass the class
     * @return node infos
     */
    public Set<JIPipeNodeInfo> getNodeInfosFromClass(Class<? extends JIPipeGraphNode> klass) {
        return new HashSet<>(registeredNodeClasses.get(klass));
    }

    public Multimap<Class<? extends JIPipeGraphNode>, JIPipeNodeInfo> getRegisteredNodeClasses() {
        return ImmutableMultimap.copyOf(registeredNodeClasses);
    }

    /**
     * Removes a node from the registry
     *
     * @param id the node id
     */
    public void unregister(String id) {
        JIPipeNodeInfo info = registeredNodeInfos.get(id);
        registeredNodeInfos.remove(id);
        registeredNodeClasses.remove(info.getInstanceClass(), info);
        registeredNodeInfoSources.remove(id);
        iconURLs.remove(info);
    }
}
