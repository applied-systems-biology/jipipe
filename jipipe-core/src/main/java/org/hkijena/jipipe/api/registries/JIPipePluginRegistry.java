/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.registries;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.utils.GraphUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Registry for managing extensions
 */
public class JIPipePluginRegistry {

    /**
     * Standard set of extension IDs (1.74.0+)
     */
    public static final String[] STANDARD_PLUGINS = new String[]{"org.hkijena.jipipe:annotations", "org.hkijena.jipipe:filesystem", "org.hkijena.jipipe:forms", "org.hkijena.jipipe:imagej-algorithms",
            "org.hkijena.jipipe:imagej-integration", "org.hkijena.jipipe:plots", "org.hkijena.jipipe:python", "org.hkijena.jipipe:r", "org.hkijena.jipipe:strings", "org.hkijena.jipipe:table-operations", "org.hkijena.jipipe:tools", "org.hkijena.jipipe:utils", "org.hkijena.jipipe:imagej2", "org.hkijena.jipipe:multi-parameters-algorithms"};

    /**
     * Standard set of extension IDs (for users updating from 1.73.x or older)
     */
    public static final String[] STANDARD_EXTENSIONS_LEGACY = new String[]{"org.hkijena.jipipe:annotations", "org.hkijena.jipipe:filesystem", "org.hkijena.jipipe:forms", "org.hkijena.jipipe:imagej-algorithms",
            "org.hkijena.jipipe:imagej-integration", "org.hkijena.jipipe:plots", "org.hkijena.jipipe:python", "org.hkijena.jipipe:r", "org.hkijena.jipipe:strings", "org.hkijena.jipipe:table-operations", "org.hkijena.jipipe:tools", "org.hkijena.jipipe:utils", "org.hkijena.jipipe:imagej2", "org.hkijena.jipipe:multi-parameters-algorithms",
            "org.hkijena.jipipe:cellpose", "org.hkijena.jipipe:clij2-integration", "org.hkijena.jipipe:ij-multi-template-matching", "org.hkijena.jipipe:ij-weka", "org.hkijena.jipipe:omero"};

    private final JIPipe jiPipe;
    private final Map<String, JIPipePlugin> knownPlugins = new HashMap<>();
    private final Set<String> scheduledActivatePlugins = new HashSet<>();
    private final Set<String> scheduledDeactivatePlugins = new HashSet<>();
    private final Set<String> newPlugins = new HashSet<>();
    private final ScheduledDeactivatePluginEventEmitter scheduledDeactivatePluginEventEmitter = new ScheduledDeactivatePluginEventEmitter();
    private final ScheduledActivatePluginEventEmitter scheduledActivatePluginEventEmitter = new ScheduledActivatePluginEventEmitter();
    private Settings settings = new Settings();
    private DefaultDirectedGraph<JIPipeDependency, DefaultEdge> dependencyGraph;


    public JIPipePluginRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    /**
     * Finds all dependencies that cannot be met
     *
     * @param dependencies List of dependencies to be checked. Only the ID will be checked.
     * @return Set of dependencies whose IDs are not registered
     */
    public static Set<JIPipeDependency> findUnsatisfiedDependencies(Set<JIPipeDependency> dependencies) {
        Set<JIPipeDependency> result = new HashSet<>();
        for (JIPipeDependency dependency : dependencies) {
            boolean found = false;
            for (JIPipeDependency registeredExtension : JIPipe.getInstance().getRegisteredExtensions()) {
                if (Objects.equals(registeredExtension.getDependencyId(), dependency.getDependencyId()) ||
                        registeredExtension.getDependencyProvides().contains(dependency.getDependencyId())) {
                    // Check version
                    if (VersionUtils.compareVersions(registeredExtension.getDependencyVersion(), dependency.getDependencyVersion()) >= 0) {
                        found = true;
                    }
                }
            }
            if (!found)
                result.add(dependency);
        }
        return result;
    }

    /**
     * @return The location of the file where the settings are stored
     */
    public static Path getPropertyFile() {
        return JIPipe.getJIPipeUserDir().resolve("plugins.json");
    }

    public void initialize() {
        settings.getActivatedPlugins().addAll(Arrays.asList(STANDARD_PLUGINS));
        if (!Files.isRegularFile(getPropertyFile()) && !JIPipe.NO_SETTINGS_AUTOSAVE) {
            save();
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public ScheduledDeactivatePluginEventEmitter getScheduledDeactivatePluginEventEmitter() {
        return scheduledDeactivatePluginEventEmitter;
    }

    public ScheduledActivatePluginEventEmitter getScheduledActivatePluginEventEmitter() {
        return scheduledActivatePluginEventEmitter;
    }

    /**
     * List of all extensions that are requested during startup
     * Please note that this may not contain core extensions
     * Use getActivatedExtensions() instead
     *
     * @return unmodifiable set
     */
    public Set<String> getStartupPlugins() {
        return Collections.unmodifiableSet(settings.getActivatedPlugins());
    }

    /**
     * List of all activated extensions
     *
     * @return unmodifiable set
     */
    public Set<String> getActivatedPlugins() {
        return Collections.unmodifiableSet(jiPipe.getRegisteredExtensionIds());
    }

    /**
     * Registers an extension as known to the extension registry.
     * Will not activate the extension
     *
     * @param extension the extension
     */
    public void registerKnownPlugin(JIPipePlugin extension) {
        jiPipe.getProgressInfo().resolve("Plugin management").log("Discovered plugin: " + extension.getDependencyId() + " version " + extension.getDependencyVersion() + " (of type " + extension.getClass().getName() + ")");
        knownPlugins.put(extension.getDependencyId(), extension);
        dependencyGraph = null;
    }

    /**
     * Returns a known extension by ID
     * If multiple extensions share the same ID, only one is returned
     * If there is none, null will be returned
     *
     * @param id the ID
     * @return the extension or null
     */
    public JIPipePlugin getKnownPluginById(String id) {
        return knownPlugins.getOrDefault(id, null);
    }

    /**
     * Updates the list of new extensions
     */
    public void findNewPlugins() {
        Set<String> activatedExtensions = getActivatedPlugins();
        for (JIPipePlugin knownExtension : getKnownPluginsList()) {
            if (!activatedExtensions.contains(knownExtension.getDependencyId()) && !settings.getSilencedPlugins().contains(knownExtension.getDependencyId())) {
                newPlugins.add(knownExtension.getDependencyId());
            }
        }
    }

    public void dismissNewPlugins() {
        settings.getSilencedPlugins().addAll(getNewPlugins());
        if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
            save();
        }
    }

    /**
     * A set of extensions that are new and should be made known to the user
     *
     * @return the extensions (unmodifiable)
     */
    public Set<String> getNewPlugins() {
        return Collections.unmodifiableSet(newPlugins);
    }

    public List<JIPipePlugin> getKnownPluginsList() {
        return new ArrayList<>(knownPlugins.values());
    }

    public Map<String, JIPipePlugin> getKnownPluginById() {
        return Collections.unmodifiableMap(knownPlugins);
    }

    public void clearSchedule(String id) {
        if (scheduledDeactivatePlugins.contains(id)) {
            scheduleActivateExtension(id);
        }
        if (scheduledActivatePlugins.contains(id)) {
            scheduleDeactivateExtension(id);
        }
    }

    /**
     * Returns the dependency IDs that are dependencies of the provided extension ID
     *
     * @param id the extension id
     * @return set of dependency ids
     */
    public Set<String> getAllDependenciesOf(String id) {
        Set<String> ids = new HashSet<>();
        for (JIPipeDependency predecessor : GraphUtils.getAllPredecessors(getDependencyGraph(), getKnownPluginById(id))) {
            ids.add(predecessor.getDependencyId());
        }
        ids.remove(id);
        return ids;
    }

    /**
     * Returns the dependency IDs that are depentents of the provided extension ID
     *
     * @param id the extension id
     * @return set of dependency ids
     */
    public Set<String> getAllDependentsOf(String id) {
        Set<String> ids = new HashSet<>();
        for (JIPipeDependency successor : GraphUtils.getAllSuccessors(getDependencyGraph(), getKnownPluginById(id))) {
            ids.add(successor.getDependencyId());
        }
        ids.remove(id);
        return ids;
    }

    public void scheduleActivateExtension(String id) {
        JIPipePlugin extension = getKnownPluginById(id);
        Set<String> ids = new HashSet<>();
        ids.add(id);
        ids.addAll(getAllDependenciesOf(id));
        for (String s : ids) {
            scheduledDeactivatePlugins.remove(s);
            scheduledActivatePlugins.add(s);
            settings.getActivatedPlugins().add(s);
            settings.getSilencedPlugins().add(s); // That the user is not warned by it
        }
        if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
            save();
        }
        for (String s : ids) {
            scheduledActivatePluginEventEmitter.emit(new ScheduledActivatePluginEvent(this, s));
        }
    }

    public void scheduleDeactivateExtension(String id) {
        JIPipePlugin extension = getKnownPluginById(id);
        Set<String> ids = new HashSet<>();
        ids.add(id);
        ids.addAll(getAllDependentsOf(id));
        for (String s : ids) {
            scheduledDeactivatePlugins.add(s);
            scheduledActivatePlugins.remove(s);
            settings.getActivatedPlugins().remove(s);
            settings.getSilencedPlugins().add(s); // That the user is not warned by it
        }
        if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
            save();
        }
        for (String s : ids) {
            scheduledDeactivatePluginEventEmitter.emit(new ScheduledDeactivatePluginEvent(this, id));
        }
    }

    /**
     * Returns a directed graph of all dependencies. The dependencies are the sources.
     * Might contain cycles
     *
     * @return the dependency graph
     */
    public DefaultDirectedGraph<JIPipeDependency, DefaultEdge> getDependencyGraph() {
        if (dependencyGraph == null) {
            dependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
            BiMap<String, JIPipeDependency> dependencyGraphNodeIds = HashBiMap.create();
            // Add the initial vertices
            for (JIPipeDependency knownExtension : knownPlugins.values()) {
                dependencyGraph.addVertex(knownExtension);
                dependencyGraphNodeIds.put(knownExtension.getDependencyId(), knownExtension);
            }
            // Add edges
            Stack<JIPipeDependency> stack = new Stack<>();
            stack.addAll(knownPlugins.values());
            while (!stack.isEmpty()) {
                JIPipeDependency target = stack.pop();
                for (JIPipeDependency source : target.getDependencies()) {
                    JIPipeDependency sourceInGraph = dependencyGraphNodeIds.getOrDefault(source.getDependencyId(), null);
                    if (sourceInGraph == null) {
                        sourceInGraph = source;
                        dependencyGraph.addVertex(sourceInGraph);
                        dependencyGraphNodeIds.put(sourceInGraph.getDependencyId(), sourceInGraph);
                    }
                    dependencyGraph.addEdge(sourceInGraph, target);
                    stack.addAll(source.getDependencies());
                }
            }
            CycleDetector<JIPipeDependency, DefaultEdge> cycleDetector = new CycleDetector<>(dependencyGraph);
            boolean hasCycles = cycleDetector.detectCycles();
            jiPipe.getProgressInfo().log("Created dependency graph: " + dependencyGraph.vertexSet().size() + " nodes, " + dependencyGraph.edgeSet().size() + " edges, has cycles: " + hasCycles);
            if (hasCycles) {
                jiPipe.getProgressInfo().log("WARNING: Cyclic dependencies detected in dependency graph!");
                jiPipe.getProgressInfo().log("Dependencies that are part of a cycle:");
                for (JIPipeDependency dependency : cycleDetector.findCycles()) {
                    jiPipe.getProgressInfo().log(" - " + dependency.getDependencyId());
                }
            }
        }
        return dependencyGraph;
    }

    public boolean isKnownDependency(String id) {
        return knownPlugins.containsKey(id);
    }

    /**
     * Resolves as many dependencies in the provided set against a known dependency, so there is access to other metadata.
     * If a dependency is not known, the original object is preserved
     *
     * @param dependencies the dependencies
     * @return set where known extensions are replacing dummy objects
     */
    public Set<JIPipeDependency> tryResolveToKnownDependencies(Set<JIPipeDependency> dependencies) {
        Set<JIPipeDependency> result = new HashSet<>();
        for (JIPipeDependency dependency : dependencies) {
            JIPipePlugin known = knownPlugins.getOrDefault(dependency.getDependencyId(), null);
            if (known == null)
                result.add(dependency);
            else
                result.add(known);
        }
        return result;
    }

    public boolean willBeActivatedOnNextStartup(String id) {
        if (getActivatedPlugins().contains(id)) {
            return !getScheduledDeactivatePlugins().contains(id);
        } else {
            return getScheduledActivatePlugins().contains(id);
        }
    }

    public boolean willBeDeactivatedOnNextStartup(String id) {
        if (getActivatedPlugins().contains(id)) {
            return getScheduledDeactivatePlugins().contains(id);
        } else {
            return !getScheduledActivatePlugins().contains(id);
        }
    }

    public Set<String> getScheduledActivatePlugins() {
        return Collections.unmodifiableSet(scheduledActivatePlugins);
    }

    public Set<String> getScheduledDeactivatePlugins() {
        return Collections.unmodifiableSet(scheduledDeactivatePlugins);
    }

    /**
     * Saves the settings to the specified file
     *
     * @param file the file path
     */
    public void save(Path file) {
        PathUtils.ensureParentDirectoriesExist(file);
        JsonUtils.saveToFile(settings, file);
    }

    /**
     * Saves the settings to the default settings file
     */
    public void save() {
        save(getPropertyFile());
    }

    public void load() {
        load(getPropertyFile());
    }

    /**
     * Loads settings from the specified file
     *
     * @param file the file
     */
    public void load(Path file) {
        if (!Files.isRegularFile(file))
            return;
        try {
            settings = JsonUtils.readFromFile(file, Settings.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JIPipe getJIPipe() {
        return jiPipe;
    }

    public interface ScheduledActivatePluginEventListener {
        void onScheduledActivatePlugin(ScheduledActivatePluginEvent event);
    }

    public interface ScheduledDeactivatePluginEventListener {
        void onScheduledDeactivatePlugin(ScheduledDeactivatePluginEvent event);
    }

    /**
     * Triggered by {@link JIPipePluginRegistry} when an extension is scheduled to be activated
     */
    public static class ScheduledActivatePluginEvent extends AbstractJIPipeEvent {
        private final String extensionId;

        public ScheduledActivatePluginEvent(JIPipePluginRegistry registry, String extensionId) {
            super(registry);
            this.extensionId = extensionId;
        }

        public String getExtensionId() {
            return extensionId;
        }
    }

    public static class ScheduledActivatePluginEventEmitter extends JIPipeEventEmitter<ScheduledActivatePluginEvent, ScheduledActivatePluginEventListener> {

        @Override
        protected void call(ScheduledActivatePluginEventListener scheduledActivatePluginEventListener, ScheduledActivatePluginEvent event) {
            scheduledActivatePluginEventListener.onScheduledActivatePlugin(event);
        }
    }

    /**
     * Triggered by {@link JIPipePluginRegistry} when an extension is scheduled to be deactivated
     */
    public static class ScheduledDeactivatePluginEvent extends AbstractJIPipeEvent {
        private final String extensionId;

        public ScheduledDeactivatePluginEvent(JIPipePluginRegistry registry, String extensionId) {
            super(registry);
            this.extensionId = extensionId;
        }

        public String getExtensionId() {
            return extensionId;
        }
    }

    public static class ScheduledDeactivatePluginEventEmitter extends JIPipeEventEmitter<ScheduledDeactivatePluginEvent, ScheduledDeactivatePluginEventListener> {

        @Override
        protected void call(ScheduledDeactivatePluginEventListener scheduledDeactivatePluginEventListener, ScheduledDeactivatePluginEvent event) {
            scheduledDeactivatePluginEventListener.onScheduledDeactivatePlugin(event);
        }
    }

    public static class Settings {
        private Set<String> activatedPlugins = new HashSet<>();

        private Set<String> silencedPlugins = new HashSet<>();

        @JsonGetter("activated-extensions")
        public Set<String> getActivatedPlugins() {
            return activatedPlugins;
        }

        @JsonSetter("activated-extensions")
        public void setActivatedPlugins(Set<String> activatedPlugins) {
            this.activatedPlugins = activatedPlugins;
        }

        @JsonGetter("silenced-extensions")
        public Set<String> getSilencedPlugins() {
            return silencedPlugins;
        }

        @JsonSetter("silenced-extensions")
        public void setSilencedPlugins(Set<String> silencedPlugins) {
            this.silencedPlugins = silencedPlugins;
        }
    }
}
