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
import com.google.common.eventbus.EventBus;
import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Registry for managing extensions
 */
public class JIPipeExtensionRegistry {

    /**
     * Standard set of extension IDs (1.74.0+)
     */
    public static final String[] STANDARD_EXTENSIONS = new String[] { "org.hkijena.jipipe:annotations", "org.hkijena.jipipe:filesystem", "org.hkijena.jipipe:forms", "org.hkijena.jipipe:imagej-algorithms",
            "org.hkijena.jipipe:imagej-integration", "org.hkijena.jipipe:plots", "org.hkijena.jipipe:python", "org.hkijena.jipipe:r", "org.hkijena.jipipe:strings", "org.hkijena.jipipe:table-operations", "org.hkijena.jipipe:tools", "org.hkijena.jipipe:utils", "org.hkijena.jipipe:imagej2", "org.hkijena.jipipe:multi-parameters-algorithms" };

    /**
     * Standard set of extension IDs (for users updating from 1.73.x or older)
     */
    public static final String[] STANDARD_EXTENSIONS_LEGACY = new String[] { "org.hkijena.jipipe:annotations", "org.hkijena.jipipe:filesystem", "org.hkijena.jipipe:forms", "org.hkijena.jipipe:imagej-algorithms",
            "org.hkijena.jipipe:imagej-integration", "org.hkijena.jipipe:plots", "org.hkijena.jipipe:python", "org.hkijena.jipipe:r", "org.hkijena.jipipe:strings", "org.hkijena.jipipe:table-operations", "org.hkijena.jipipe:tools", "org.hkijena.jipipe:utils", "org.hkijena.jipipe:imagej2", "org.hkijena.jipipe:multi-parameters-algorithms",
            "org.hkijena.jipipe:cellpose", "org.hkijena.jipipe:clij2-integration", "org.hkijena.jipipe:ij-multi-template-matching", "org.hkijena.jipipe:ij-weka", "org.hkijena.jipipe:omero"};

    private final JIPipe jiPipe;
    private final EventBus eventBus = new EventBus();

    private Settings settings = new Settings();

    private final List<JIPipeExtension> knownExtensions = new ArrayList<>();

    private final Set<String> scheduledActivateExtensions = new HashSet<>();

    private final Set<String> scheduledDeactivateExtensions = new HashSet<>();

    private final Set<String> newExtensions = new HashSet<>();

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
            boolean found = JIPipe.getInstance().getRegisteredExtensions().stream().anyMatch(d -> d.getDependencyId().equals(dependency.getDependencyId()));
            if (!found)
                result.add(dependency);
        }
        return result;
    }

    public void initialize() {
        if(isLegacy()) {
            settings.getActivatedExtensions().addAll(Arrays.asList(STANDARD_EXTENSIONS_LEGACY));
        }
        else {
            settings.getActivatedExtensions().addAll(Arrays.asList(STANDARD_EXTENSIONS));
        }
        if(!Files.isRegularFile(getPropertyFile())) {
            save();
        }
    }

    public static boolean isLegacy() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        return Files.isRegularFile(imageJDir.resolve("jipipe.properties.json"));
    }

    /**
     * List of all extensions that are requested during startup
     * Please note that this may not contain core extensions
     * Use getActivatedExtensions() instead
     * @return unmodifiable set
     */
    public Set<String> getStartupExtensions() {
        return Collections.unmodifiableSet(settings.getActivatedExtensions());
    }

    /**
     * List of all activated extensions
     * @return unmodifiable set
     */
    public Set<String> getActivatedExtensions() {
        return Collections.unmodifiableSet(jiPipe.getRegisteredExtensionIds());
    }

    /**
     * Registers an extension as known to the extension registry.
     * Will not activate the extension
     * @param extension the extension
     */
    public void registerKnownExtension(JIPipeExtension extension) {
        jiPipe.getProgressInfo().resolve("Extension management").log("Discovered extension: " + extension.getDependencyId() + " version " + extension.getDependencyVersion() + " (of type " + extension.getClass().getName() + ")" );
        knownExtensions.add(extension);
        dependencyGraph = null;
    }

    /**
     * Returns a known extension by ID
     * If multiple extensions share the same ID, only one is returned
     * If there is none, null will be returned
     * @param id the ID
     * @return the extension or null
     */
    public JIPipeExtension getKnownExtensionById(String id) {
        return knownExtensions.stream().filter(dependency -> Objects.equals(dependency.getDependencyId(), id)).findFirst().orElse(null);
    }

    /**
     * Updates the list of new extensions
     */
    public void findNewExtensions() {
        Set<String> activatedExtensions = getActivatedExtensions();
        for (JIPipeExtension knownExtension : getKnownExtensions()) {
            if(!activatedExtensions.contains(knownExtension.getDependencyId()) && !settings.getSilencedExtensions().contains(knownExtension.getDependencyId())) {
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
     * @return the extensions (unmodifiable)
     */
    public Set<String> getNewExtensions() {
        return Collections.unmodifiableSet(newExtensions);
    }

    public List<JIPipeExtension> getKnownExtensions() {
        return Collections.unmodifiableList(knownExtensions);
    }

    public void clearSchedule(String id) {
        if(scheduledDeactivateExtensions.contains(id)) {
            scheduledDeactivateExtensions.remove(id);
            eventBus.post(new ScheduledActivateExtension(id));
        }
        if(scheduledActivateExtensions.contains(id)) {
            scheduledActivateExtensions.remove(id);
            eventBus.post(new ScheduledDeactivateExtension(id));
        }
    }

    public void scheduleActivateExtension(String id) {
        scheduledDeactivateExtensions.remove(id);
        scheduledActivateExtensions.add(id);
        settings.getActivatedExtensions().add(id);
        settings.getSilencedExtensions().add(id); // That the user is not warned by it
        save();
        eventBus.post(new ScheduledActivateExtension(id));
    }

    public void scheduleDeactivateExtension(String id) {
        scheduledDeactivateExtensions.add(id);
        scheduledActivateExtensions.remove(id);
        settings.getActivatedExtensions().remove(id);
        settings.getSilencedExtensions().add(id); // That the user is not warned by it
        save();
        eventBus.post(new ScheduledDeactivateExtension(id));
    }

    /**
     * Returns a graph of all dependencies.
     * Might contain cycles
     * @return the dependency graph
     */
    public DefaultDirectedGraph<JIPipeDependency, DefaultEdge> getDependencyGraph() {
        if(dependencyGraph == null) {
            dependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
            BiMap<String, JIPipeDependency> dependencyGraphNodeIds = HashBiMap.create();
            // Add the initial vertices
            for (JIPipeDependency knownExtension : knownExtensions) {
                dependencyGraph.addVertex(knownExtension);
                dependencyGraphNodeIds.put(knownExtension.getDependencyId(), knownExtension);
            }
            // Add edges
            Stack<JIPipeDependency> stack = new Stack<>();
            stack.addAll(knownExtensions);
            while(!stack.isEmpty()) {
                JIPipeDependency target = stack.pop();
                for (JIPipeDependency source : target.getDependencies()) {
                    JIPipeDependency sourceInGraph = dependencyGraphNodeIds.getOrDefault(source.getDependencyId(), null);
                    if(sourceInGraph == null) {
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
            if(hasCycles)
                jiPipe.getProgressInfo().log("WARNING: Cyclic dependencies detected in dependency graph!");
        }
        return dependencyGraph;
    }

    public Set<String> getScheduledActivateExtensions() {
        return Collections.unmodifiableSet(scheduledActivateExtensions);
    }

    public Set<String> getScheduledDeactivateExtensions() {
        return Collections.unmodifiableSet(scheduledDeactivateExtensions);
    }

    /**
     * @return The location of the file where the settings are stored
     */
    public static Path getPropertyFile() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        return imageJDir.resolve("jipipe.extensions.json");
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

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Triggered by {@link JIPipeExtensionRegistry} when an extension is scheduled to be activated
     */
    public static class ScheduledActivateExtension {
        private final String extensionId;

        public ScheduledActivateExtension(String extensionId) {
            this.extensionId = extensionId;
        }

        public String getExtensionId() {
            return extensionId;
        }
    }

    /**
     * Triggered by {@link JIPipeExtensionRegistry} when an extension is scheduled to be deactivated
     */
    public static class ScheduledDeactivateExtension {
        private final String extensionId;

        public ScheduledDeactivateExtension(String extensionId) {
            this.extensionId = extensionId;
        }

        public String getExtensionId() {
            return extensionId;
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
