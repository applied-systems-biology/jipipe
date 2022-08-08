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

import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.plugin.ZProjector;
import org.antlr.misc.MultiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class JIPipeNodeRegistry implements JIPipeValidatable {
    private final Map<String, JIPipeNodeInfo> registeredNodeInfos = new HashMap<>();
    private final Multimap<Class<? extends JIPipeGraphNode>, JIPipeNodeInfo> registeredNodeClasses = HashMultimap.create();

    private final Multimap<String, JIPipeNodeExample> registeredExamples = HashMultimap.create();
    private final Set<JIPipeNodeRegistrationTask> registrationTasks = new HashSet<>();
    private final Map<String, JIPipeDependency> registeredNodeInfoSources = new HashMap<>();
    private final BiMap<String, JIPipeNodeTypeCategory> registeredCategories = HashBiMap.create();
    private final Map<JIPipeNodeInfo, URL> iconURLs = new HashMap<>();
    private final Map<JIPipeNodeInfo, ImageIcon> iconInstances = new HashMap<>();

    private final List<JIPipeNodeTemplate> scheduledRegisterExamples = new ArrayList<>();
    private final EventBus eventBus = new EventBus();
    private final URL defaultIconURL;
    private final JIPipe jiPipe;
    private boolean stateChanged;
    private boolean isRunning;

    /**
     * Creates a new registry
     *
     * @param jiPipe the JIPipe instance
     */
    public JIPipeNodeRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
        this.defaultIconURL = ResourceUtils.getPluginResource("icons/actions/configure.png");
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
        getJIPipe().getProgressInfo().log("Registered node type '" + info.getName() + "' [" + info.getId() + "]");
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
        getJIPipe().getProgressInfo().log("Registered node type category " + category);
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
     * @param category             The category
     * @param includingAlternative if alternative categorizations should be included (from alternative menu paths)
     * @return Algorithms within the specified category
     */
    public Set<JIPipeNodeInfo> getNodesOfCategory(JIPipeNodeTypeCategory category, boolean includingAlternative) {
        return registeredNodeInfos.values().stream().filter(d -> {
            if (includingAlternative) {
                for (JIPipeNodeMenuLocation location : d.getAliases()) {
                    if (Objects.equals(location.getCategory().getId(), category.getId())) {
                        return true;
                    }
                }
            }
            return Objects.equals(d.getCategory().getId(), category.getId());
        }).collect(Collectors.toSet());
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
    public void reportValidity(JIPipeIssueReport report) {
        for (JIPipeNodeRegistrationTask task : registrationTasks) {
            report.resolve("Unregistered algorithms").resolve(task.toString()).report(task);
        }
    }

    public ImmutableBiMap<String, JIPipeNodeTypeCategory> getRegisteredCategories() {
        return ImmutableBiMap.copyOf(registeredCategories);
    }

    /**
     * Registers a custom icon for a node
     *
     * @param info         the node type
     * @param resourcePath icon url
     */
    public void registerIcon(JIPipeNodeInfo info, URL resourcePath) {
        if(resourcePath == null) {
            jiPipe.getProgressInfo().log("Unable to register icon for " + info.getId() + ": URL is null.");
            return;
        }
        iconURLs.put(info, resourcePath);
        iconInstances.put(info, new ImageIcon(resourcePath));
    }

    /**
     * Registers a node template as example
     * Will reject
     * @param nodeTemplate the node template that contains the node example
     */
    public void registerExample(JIPipeNodeTemplate nodeTemplate) {
        JIPipeNodeExample example = new JIPipeNodeExample(nodeTemplate);
        if(example.getNodeId() == null) {
            jiPipe.getProgressInfo().log("ERROR: Unable to register node template '" + nodeTemplate.getName() + "'. No [unique] node ID.");
        }
        else {
            jiPipe.getProgressInfo().log("Registered example for " + example.getNodeId() + ": '" + nodeTemplate.getName() + "'");
            registeredExamples.put(example.getNodeId(), example);
        }
    }

    /**
     * Returns the icon resource path URL for a node
     *
     * @param info node type
     * @return icon url
     */
    public URL getIconURLFor(JIPipeNodeInfo info) {
        return iconURLs.getOrDefault(info, defaultIconURL);
    }

    /**
     * Returns the icon for a node
     *
     * @param info node type
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

    public Collection<JIPipeNodeExample> getNodeExamples(String nodeTypeId) {
        return registeredExamples.get(nodeTypeId);
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

    public JIPipe getJIPipe() {
        return jiPipe;
    }

    public void scheduleRegisterExample(JIPipeNodeTemplate template) {
        jiPipe.getProgressInfo().log("Scheduled node template '" + template.getName() + "' to be registered as example.");
        scheduledRegisterExamples.add(template);
    }

    public void executeScheduledRegisterExamples() {
        for (JIPipeNodeTemplate example : scheduledRegisterExamples) {
            registerExample(example);
        }
        scheduledRegisterExamples.clear();
    }
}
