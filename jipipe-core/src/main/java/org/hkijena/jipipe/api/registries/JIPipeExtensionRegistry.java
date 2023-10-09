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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.utils.GraphUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Registry for managing extensions
 */
public class JIPipeExtensionRegistry {

    /**
     * Standard set of extension IDs (1.74.0+)
     */
    public static final String[] STANDARD_EXTENSIONS = new String[]{"org.hkijena.jipipe:annotations", "org.hkijena.jipipe:filesystem", "org.hkijena.jipipe:forms", "org.hkijena.jipipe:imagej-algorithms",
            "org.hkijena.jipipe:imagej-integration", "org.hkijena.jipipe:plots", "org.hkijena.jipipe:python", "org.hkijena.jipipe:r", "org.hkijena.jipipe:strings", "org.hkijena.jipipe:table-operations", "org.hkijena.jipipe:tools", "org.hkijena.jipipe:utils", "org.hkijena.jipipe:imagej2", "org.hkijena.jipipe:multi-parameters-algorithms"};

    /**
     * Standard set of extension IDs (for users updating from 1.73.x or older)
     */
    public static final String[] STANDARD_EXTENSIONS_LEGACY = new String[]{"org.hkijena.jipipe:annotations", "org.hkijena.jipipe:filesystem", "org.hkijena.jipipe:forms", "org.hkijena.jipipe:imagej-algorithms",
            "org.hkijena.jipipe:imagej-integration", "org.hkijena.jipipe:plots", "org.hkijena.jipipe:python", "org.hkijena.jipipe:r", "org.hkijena.jipipe:strings", "org.hkijena.jipipe:table-operations", "org.hkijena.jipipe:tools", "org.hkijena.jipipe:utils", "org.hkijena.jipipe:imagej2", "org.hkijena.jipipe:multi-parameters-algorithms",
            "org.hkijena.jipipe:cellpose", "org.hkijena.jipipe:clij2-integration", "org.hkijena.jipipe:ij-multi-template-matching", "org.hkijena.jipipe:ij-weka", "org.hkijena.jipipe:omero"};

    private final JIPipe jiPipe;
    private final Map<String, JIPipeExtension> knownExtensions = new HashMap<>();
    private final Set<String> scheduledActivateExtensions = new HashSet<>();
    private final Set<String> scheduledDeactivateExtensions = new HashSet<>();
    private final Set<String> newExtensions = new HashSet<>();
    private final ScheduledDeactivateExtensionEventEmitter scheduledDeactivateExtensionEventEmitter = new ScheduledDeactivateExtensionEventEmitter();
    private final ScheduledActivateExtensionEventEmitter scheduledActivateExtensionEventEmitter = new ScheduledActivateExtensionEventEmitter();
    private Settings settings = new Settings();
    private DefaultDirectedGraph<JIPipeDependency, DefaultEdge> dependencyGraph;


    public JIPipeExtensionRegistry(JIPipe jiPipe) {
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
                if(registeredExtension.getDependencyId().equals(dependency.getDependencyId())) {
                    // Check version
                    if(VersionUtils.compareVersions(registeredExtension.getDependencyVersion(), dependency.getDependencyVersion()) >= 0) {
                        found = true;
                    }
                }
            }
            if (!found)
                result.add(dependency);
        }
        return result;
    }

    public static boolean isLegacy() {
        return Files.isRegularFile(JIPipe.getJIPipeUserDir().resolve("jipipe.properties.json"));
    }

    /**
     * @return The location of the file where the settings are stored
     */
    public static Path getPropertyFile() {
        return JIPipe.getJIPipeUserDir().resolve("jipipe.extensions.json");
    }

    public void initialize() {
        if (isLegacy()) {
            settings.getActivatedExtensions().addAll(Arrays.asList(STANDARD_EXTENSIONS_LEGACY));
        } else {
            settings.getActivatedExtensions().addAll(Arrays.asList(STANDARD_EXTENSIONS));
        }
        if (!Files.isRegularFile(getPropertyFile())) {
            save();
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public ScheduledDeactivateExtensionEventEmitter getScheduledDeactivateExtensionEventEmitter() {
        return scheduledDeactivateExtensionEventEmitter;
    }

    public ScheduledActivateExtensionEventEmitter getScheduledActivateExtensionEventEmitter() {
        return scheduledActivateExtensionEventEmitter;
    }

    /**
     * List of all extensions that are requested during startup
     * Please note that this may not contain core extensions
     * Use getActivatedExtensions() instead
     *
     * @return unmodifiable set
     */
    public Set<String> getStartupExtensions() {
        return Collections.unmodifiableSet(settings.getActivatedExtensions());
    }

    /**
     * List of all activated extensions
     *
     * @return unmodifiable set
     */
    public Set<String> getActivatedExtensions() {
        return Collections.unmodifiableSet(jiPipe.getRegisteredExtensionIds());
    }

    /**
     * Registers an extension as known to the extension registry.
     * Will not activate the extension
     *
     * @param extension the extension
     */
    public void registerKnownExtension(JIPipeExtension extension) {
        jiPipe.getProgressInfo().resolve("Extension management").log("Discovered extension: " + extension.getDependencyId() + " version " + extension.getDependencyVersion() + " (of type " + extension.getClass().getName() + ")");
        knownExtensions.put(extension.getDependencyId(), extension);
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
    public JIPipeExtension getKnownExtensionById(String id) {
        return knownExtensions.getOrDefault(id, null);
    }

    /**
     * Updates the list of new extensions
     */
    public void findNewExtensions() {
        Set<String> activatedExtensions = getActivatedExtensions();
        for (JIPipeExtension knownExtension : getKnownExtensionsList()) {
            if (!activatedExtensions.contains(knownExtension.getDependencyId()) && !settings.getSilencedExtensions().contains(knownExtension.getDependencyId())) {
                newExtensions.add(knownExtension.getDependencyId());
            }
        }
    }

    public void dismissNewExtensions() {
        settings.getSilencedExtensions().addAll(getNewExtensions());
        save();
    }

    /**
     * A set of extensions that are new and should be made known to the user
     *
     * @return the extensions (unmodifiable)
     */
    public Set<String> getNewExtensions() {
        return Collections.unmodifiableSet(newExtensions);
    }

    public List<JIPipeExtension> getKnownExtensionsList() {
        return new ArrayList<>(knownExtensions.values());
    }

    public Map<String, JIPipeExtension> getKnownExtensionsById() {
        return Collections.unmodifiableMap(knownExtensions);
    }

    public void clearSchedule(String id) {
        if (scheduledDeactivateExtensions.contains(id)) {
            scheduleActivateExtension(id);
        }
        if (scheduledActivateExtensions.contains(id)) {
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
        for (JIPipeDependency predecessor : GraphUtils.getAllPredecessors(getDependencyGraph(), getKnownExtensionById(id))) {
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
        for (JIPipeDependency successor : GraphUtils.getAllSuccessors(getDependencyGraph(), getKnownExtensionById(id))) {
            ids.add(successor.getDependencyId());
        }
        ids.remove(id);
        return ids;
    }

    public void scheduleActivateExtension(String id) {
        JIPipeExtension extension = getKnownExtensionById(id);
        Set<String> ids = new HashSet<>();
        ids.add(id);
        ids.addAll(getAllDependenciesOf(id));
        for (String s : ids) {
            scheduledDeactivateExtensions.remove(s);
            scheduledActivateExtensions.add(s);
            settings.getActivatedExtensions().add(s);
            settings.getSilencedExtensions().add(s); // That the user is not warned by it
        }
        save();
        for (String s : ids) {
            scheduledActivateExtensionEventEmitter.emit(new ScheduledActivateExtensionEvent(this, s));
        }
    }

    public void scheduleDeactivateExtension(String id) {
        JIPipeExtension extension = getKnownExtensionById(id);
        Set<String> ids = new HashSet<>();
        ids.add(id);
        ids.addAll(getAllDependentsOf(id));
        for (String s : ids) {
            scheduledDeactivateExtensions.add(s);
            scheduledActivateExtensions.remove(s);
            settings.getActivatedExtensions().remove(s);
            settings.getSilencedExtensions().add(s); // That the user is not warned by it
        }
        save();
        for (String s : ids) {
            scheduledDeactivateExtensionEventEmitter.emit(new ScheduledDeactivateExtensionEvent(this, id));
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
            for (JIPipeDependency knownExtension : knownExtensions.values()) {
                dependencyGraph.addVertex(knownExtension);
                dependencyGraphNodeIds.put(knownExtension.getDependencyId(), knownExtension);
            }
            // Add edges
            Stack<JIPipeDependency> stack = new Stack<>();
            stack.addAll(knownExtensions.values());
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
        return knownExtensions.containsKey(id);
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
            JIPipeExtension known = knownExtensions.getOrDefault(dependency.getDependencyId(), null);
            if (known == null)
                result.add(dependency);
            else
                result.add(known);
        }
        return result;
    }

    public boolean willBeActivatedOnNextStartup(String id) {
        if (getActivatedExtensions().contains(id)) {
            return !getScheduledDeactivateExtensions().contains(id);
        } else {
            return getScheduledActivateExtensions().contains(id);
        }
    }

    public boolean willBeDeactivatedOnNextStartup(String id) {
        if (getActivatedExtensions().contains(id)) {
            return getScheduledDeactivateExtensions().contains(id);
        } else {
            return !getScheduledActivateExtensions().contains(id);
        }
    }

    public Set<String> getScheduledActivateExtensions() {
        return Collections.unmodifiableSet(scheduledActivateExtensions);
    }

    public Set<String> getScheduledDeactivateExtensions() {
        return Collections.unmodifiableSet(scheduledDeactivateExtensions);
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

    public interface ScheduledActivateExtensionEventListener {
        void onScheduledActivateExtension(ScheduledActivateExtensionEvent event);
    }

    public interface ScheduledDeactivateExtensionEventListener {
        void onScheduledDeactivateExtension(ScheduledDeactivateExtensionEvent event);
    }

    /**
     * Triggered by {@link JIPipeExtensionRegistry} when an extension is scheduled to be activated
     */
    public static class ScheduledActivateExtensionEvent extends AbstractJIPipeEvent {
        private final String extensionId;

        public ScheduledActivateExtensionEvent(JIPipeExtensionRegistry registry, String extensionId) {
            super(registry);
            this.extensionId = extensionId;
        }

        public String getExtensionId() {
            return extensionId;
        }
    }

    public static class ScheduledActivateExtensionEventEmitter extends JIPipeEventEmitter<ScheduledActivateExtensionEvent, ScheduledActivateExtensionEventListener> {

        @Override
        protected void call(ScheduledActivateExtensionEventListener scheduledActivateExtensionEventListener, ScheduledActivateExtensionEvent event) {
            scheduledActivateExtensionEventListener.onScheduledActivateExtension(event);
        }
    }

    /**
     * Triggered by {@link JIPipeExtensionRegistry} when an extension is scheduled to be deactivated
     */
    public static class ScheduledDeactivateExtensionEvent extends AbstractJIPipeEvent {
        private final String extensionId;

        public ScheduledDeactivateExtensionEvent(JIPipeExtensionRegistry registry, String extensionId) {
            super(registry);
            this.extensionId = extensionId;
        }

        public String getExtensionId() {
            return extensionId;
        }
    }

    public static class ScheduledDeactivateExtensionEventEmitter extends JIPipeEventEmitter<ScheduledDeactivateExtensionEvent, ScheduledDeactivateExtensionEventListener> {

        @Override
        protected void call(ScheduledDeactivateExtensionEventListener scheduledDeactivateExtensionEventListener, ScheduledDeactivateExtensionEvent event) {
            scheduledDeactivateExtensionEventListener.onScheduledDeactivateExtension(event);
        }
    }

    public static class Settings {
        private Set<String> activatedExtensions = new HashSet<>();

        private Set<String> silencedExtensions = new HashSet<>();

        @JsonGetter("activated-extensions")
        public Set<String> getActivatedExtensions() {
            return activatedExtensions;
        }

        @JsonSetter("activated-extensions")
        public void setActivatedExtensions(Set<String> activatedExtensions) {
            this.activatedExtensions = activatedExtensions;
        }

        @JsonGetter("silenced-extensions")
        public Set<String> getSilencedExtensions() {
            return silencedExtensions;
        }

        @JsonSetter("silenced-extensions")
        public void setSilencedExtensions(Set<String> silencedExtensions) {
            this.silencedExtensions = silencedExtensions;
        }
    }
}
