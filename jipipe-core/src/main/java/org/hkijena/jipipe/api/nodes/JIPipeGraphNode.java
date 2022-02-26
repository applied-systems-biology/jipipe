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

package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * A node is a set of input and output data slots, and a run() function
 * It is part of the {@link JIPipeGraph}.
 * The workload of this node is considered optional (this means it can have an empty workload),
 * which is important for structural graphs like the compartment graph.
 * Use {@link JIPipeAlgorithm} as base class to indicate a non-optional workload.
 */
@JsonSerialize(using = JIPipeGraphNode.Serializer.class)
public abstract class JIPipeGraphNode implements JIPipeValidatable, JIPipeParameterCollection {
    private JIPipeNodeInfo info;
    private JIPipeSlotConfiguration slotConfiguration;
    private List<JIPipeDataSlot> inputSlots = new ArrayList<>();
    private List<JIPipeDataSlot> outputSlots = new ArrayList<>();
    private BiMap<String, JIPipeDataSlot> inputSlotMap = HashBiMap.create();
    private BiMap<String, JIPipeDataSlot> outputSlotMap = HashBiMap.create();
    private EventBus eventBus = new EventBus();
    private Map<String, Map<String, Point>> locations = new HashMap<>();
    private Path internalStoragePath;
    private Path storagePath;
    private String customName;
    private HTMLText customDescription;
    private JIPipeGraph graph;
    private Path projectWorkDirectory;
    private Path scratchBaseDirectory;
    private boolean bookmarked;

    /**
     * Initializes this algorithm with a custom provided slot configuration
     *
     * @param info              Contains algorithm metadata
     * @param slotConfiguration if null, generate the slot configuration
     */
    public JIPipeGraphNode(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        this.info = info;
        if (slotConfiguration == null) {
            JIPipeDefaultMutableSlotConfiguration.Builder builder = JIPipeDefaultMutableSlotConfiguration.builder();

            for (JIPipeInputSlot slot : getClass().getAnnotationsByType(JIPipeInputSlot.class)) {
                if (slot.autoCreate()) {
                    builder.addInputSlot(slot.slotName(), slot.description(), slot.value(), slot.optional());
                }
            }
            for (JIPipeOutputSlot slot : getClass().getAnnotationsByType(JIPipeOutputSlot.class)) {
                if (slot.autoCreate()) {
                    if (!slot.inheritedSlot().isEmpty())
                        builder.allowOutputSlotInheritance(true);
                    builder.addOutputSlot(slot.slotName(), slot.description(), slot.value(), slot.inheritedSlot());
                }
            }

            builder.seal();
            slotConfiguration = builder.build();
        }

        this.slotConfiguration = slotConfiguration;
        slotConfiguration.getEventBus().register(this);
        updateGraphNodeSlots();
    }

    /**
     * Initializes a new node type instance
     *
     * @param info The algorithm info
     */
    public JIPipeGraphNode(JIPipeNodeInfo info) {
        this(info, null);
    }

    /**
     * Copies the input algorithm's properties into this algorithm
     *
     * @param other Copied algorithm
     */
    public JIPipeGraphNode(JIPipeGraphNode other) {
        this.info = other.info;
        this.bookmarked = other.bookmarked;
        this.slotConfiguration = copySlotConfiguration(other);
        this.locations = new HashMap<>();
        for (Map.Entry<String, Map<String, Point>> entry : other.locations.entrySet()) {
            locations.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        this.customName = other.customName;
        this.customDescription = other.customDescription;
        updateGraphNodeSlots();
        slotConfiguration.getEventBus().register(this);
    }

    public static <T extends JIPipeGraphNode> T fromJsonNode(JsonNode node, JIPipeIssueReport issues) {
        String id = node.get("jipipe:node-info-id").asText();
        if (!JIPipe.getNodes().hasNodeInfoWithId(id)) {
            System.err.println("Unable to find node type with ID '" + id + "'. Skipping.");
            issues.resolve("Nodes").resolve(id).reportIsInvalid("Unable to find node type '" + id + "'!",
                    "The JSON data requested to load a node of type '" + id + "', but it is not known to JIPipe.",
                    "Please check if all extensions are are correctly loaded.",
                    node);
            return null;
        }
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(id);
        JIPipeGraphNode algorithm = info.newInstance();
        algorithm.fromJson(node, issues.resolve("Nodes").resolve(id));
        return (T) algorithm;
    }

    /**
     * Synchronizes the slots with the slot definition
     */
    public void updateGraphNodeSlots() {
        boolean changed = false;

        // This loop handles add & replace
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : slotConfiguration.getInputSlots().entrySet()) {
            changed |= updateAddAndModifySlots(changed, entry, inputSlotMap, inputSlots);
        }
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : slotConfiguration.getOutputSlots().entrySet()) {
            changed |= updateAddAndModifySlots(changed, entry, outputSlotMap, outputSlots);
        }

        // This loop handles remove
        changed |= updateRemoveSlots(changed, slotConfiguration.getInputSlots(), inputSlotMap, inputSlots);
        changed |= updateRemoveSlots(changed, slotConfiguration.getOutputSlots(), outputSlotMap, outputSlots);

        // Update according to slot order
        changed |= updateSlotOrder(slotConfiguration.getInputSlotOrder(), inputSlotMap, inputSlots);
        changed |= updateSlotOrder(slotConfiguration.getOutputSlotOrder(), outputSlotMap, outputSlots);

        if (changed) {
            eventBus.post(new JIPipeGraph.NodeSlotsChangedEvent(this));
            updateSlotInheritance();
        }
    }

    private boolean updateSlotOrder(List<String> configurationOrder, BiMap<String, JIPipeDataSlot> slotMap, List<JIPipeDataSlot> slots) {
        if (configurationOrder.size() == slots.size()) {
            boolean noChanges = true;
            for (int i = 0; i < configurationOrder.size(); i++) {
                if (!Objects.equals(configurationOrder.get(i), slots.get(i).getName())) {
                    noChanges = false;
                    break;
                }
            }
            if (noChanges)
                return false;
        }

        slots.clear();
        for (String name : configurationOrder) {
            JIPipeDataSlot instance = slotMap.getOrDefault(name, null);
            if (instance != null) {
                slots.add(instance);
            }
        }
        for (JIPipeDataSlot instance : slotMap.values()) {
            if (!slots.contains(instance)) {
                slots.add(instance);
            }
        }
        return true;
    }

    private boolean updateRemoveSlots(boolean changed, Map<String, JIPipeDataSlotInfo> definedSlots, BiMap<String, JIPipeDataSlot> slotMap, List<JIPipeDataSlot> slots) {
        for (String name : ImmutableList.copyOf(slotMap.keySet())) {
            if (!definedSlots.containsKey(name)) {
                JIPipeDataSlot existing = slotMap.get(name);
                slotMap.remove(name);
                slots.remove(existing);
                changed = true;
            }
        }
        return changed;
    }

    private boolean updateAddAndModifySlots(boolean changed, Map.Entry<String, JIPipeDataSlotInfo> entry, BiMap<String, JIPipeDataSlot> slotMap, List<JIPipeDataSlot> slots) {
        JIPipeDataSlot existing = slotMap.getOrDefault(entry.getKey(), null);
        if (existing != null) {
            if (!Objects.equals(existing.getInfo(), entry.getValue())) {
                slotMap.remove(entry.getKey());
                slots.remove(existing);
                existing = null;
            }
        }
        if (existing == null) {
            existing = new JIPipeDataSlot(entry.getValue(), this);
            slotMap.put(entry.getKey(), existing);
            slots.add(existing);
            changed = true;
        }
        return changed;
    }

    /**
     * Copies the slot configuration from the other algorithm to this algorithm
     * Override this method for special configuration cases
     *
     * @param other Copied slot configuration
     * @return Copy
     */
    protected JIPipeSlotConfiguration copySlotConfiguration(JIPipeGraphNode other) {
        JIPipeDefaultMutableSlotConfiguration configuration = JIPipeDefaultMutableSlotConfiguration.builder().build();
        configuration.setTo(other.slotConfiguration);
        return configuration;
    }

    /**
     * Runs the workload
     *
     * @param progressInfo progress passed from the runner
     */
    public abstract void run(JIPipeProgressInfo progressInfo);

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the algorithm name
     *
     * @return algorithm name
     */
    @JIPipeParameter(value = "jipipe:node:name", uiOrder = -9999)
    @JIPipeDocumentation(name = "Name", description = "Custom algorithm name.")
    public String getName() {
        if (customName == null || customName.isEmpty())
            return getInfo().getName();
        return customName;
    }

    /**
     * Sets a custom name. If set to null, the standard algorithm name is automatically used by getName()
     *
     * @param customName custom name
     */
    @JIPipeParameter("jipipe:node:name")
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (ParameterUtils.isHiddenLocalParameter(tree, access, "jipipe:node:description", "jipipe:node:name")) {
            return false;
        }
        return JIPipeParameterCollection.super.isParameterUIVisible(tree, access);
    }

    @JIPipeDocumentation(name = "Bookmark this node", description = "If enabled, the node is highlighted in the graph editor UI and added into the bookmark list.")
    @JIPipeParameter("jipipe:node:bookmarked")
    public boolean isBookmarked() {
        return bookmarked;
    }

    @JIPipeParameter("jipipe:node:bookmarked")
    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    /**
     * Gets the algorithm category
     *
     * @return The category
     */
    public JIPipeNodeTypeCategory getCategory() {
        return getInfo().getCategory();
    }

    /**
     * Gets the slot configuration
     *
     * @return Slot configuration
     */
    public JIPipeSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public void setSlotConfiguration(JIPipeSlotConfiguration slotConfiguration) {
        this.slotConfiguration = slotConfiguration;
    }

    /**
     * Gets all input slot instances
     *
     * @return Current algorithm slots
     */
    public Map<String, JIPipeDataSlot> getInputSlotMap() {
        return Collections.unmodifiableMap(inputSlotMap);
    }

    /**
     * Gets all output slot instances
     *
     * @return Current algorithm slots
     */
    public Map<String, JIPipeDataSlot> getOutputSlotMap() {
        return Collections.unmodifiableMap(outputSlotMap);
    }

    /**
     * Gets the input slot order
     *
     * @return List of slot names
     */
    public List<String> getInputSlotOrder() {
        return getSlotConfiguration().getInputSlotOrder();
    }

    /**
     * Returns all input slots ordered by the slot order
     *
     * @return List of slots
     */
    public List<JIPipeDataSlot> getInputSlots() {
        return Collections.unmodifiableList(inputSlots);
    }

    /**
     * Method that can be overwritten by child classes.
     * Should return slots that are actually used as input.
     *
     * @return Input slots
     */
    public List<JIPipeDataSlot> getEffectiveInputSlots() {
        return getInputSlots();
    }

    /**
     * Returns all output slots ordered by the slot order
     *
     * @return List of slots
     */
    public List<JIPipeDataSlot> getOutputSlots() {
        return Collections.unmodifiableList(outputSlots);
    }

    /**
     * Triggered when a parameter is changed within the slot configuration.
     * Triggers a {@link JIPipeGraph.NodeSlotsChangedEvent}
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterChanged(ParameterChangedEvent event) {
        if (event.getSource() == slotConfiguration) {
            getEventBus().post(new JIPipeGraph.NodeSlotsChangedEvent(this));
        } else if (event.getSource() != this) {
            getEventBus().post(event);
        }
    }

    /**
     * Should be triggered when a slot is added to the slot configuration
     *
     * @param event The event
     */
    @Subscribe
    public void onSlotConfigurationChanged(JIPipeSlotConfiguration.SlotsChangedEvent event) {
        if (event.getConfiguration() == getSlotConfiguration()) {
            updateGraphNodeSlots();
        }
    }

    /**
     * Gets the location map (writable) as map from compartment UUID to visual mode to location
     *
     * @return map from compartment UUID to visual mode to location
     */
    public Map<String, Map<String, Point>> getLocations() {
        return locations;
    }

    /**
     * Sets the location map.
     *
     * @param locations map from compartment UUID to visual mode to location
     */
    public void setLocations(Map<String, Map<String, Point>> locations) {
        this.locations = locations;
    }

    /**
     * Returns the location within the specified compartment or null if none is set
     *
     * @param compartment The compartment ID
     * @param visualMode  Used to differentiate between different visual modes
     * @return The UI location or null if unset
     */
    public Point getLocationWithin(String compartment, String visualMode) {
        Map<String, Point> visualModeMap = locations.getOrDefault(compartment, null);
        if (visualModeMap != null) {
            return visualModeMap.getOrDefault(visualMode, null);
        }
        return null;
    }

    /**
     * Sets the UI location of this algorithm within the specified compartment
     *
     * @param compartment The compartment ID
     * @param location    The UI location. Can be null to reset the location
     * @param visualMode  Used to differentiate between different visual modes
     */
    public void setLocationWithin(String compartment, Point location, String visualMode) {
        Map<String, Point> visualModeMap = locations.getOrDefault(compartment, null);
        if (visualModeMap == null) {
            visualModeMap = new HashMap<>();
            locations.put(compartment, visualModeMap);
        }
        visualModeMap.put(visualMode, location);
    }

    /**
     * Sets the UI location of this algorithm within the specified compartment
     *
     * @param compartment The compartment ID
     * @param location    The UI location. Can be null to reset the location
     * @param visualMode  Used to differentiate between different visual modes
     */
    public void setLocationWithin(UUID compartment, Point location, String visualMode) {
        setLocationWithin(StringUtils.nullToEmpty(compartment), location, visualMode);
    }

    /**
     * Saves this algorithm to JSON.
     * Override this method to apply your own modifications
     *
     * @param jsonGenerator the JSON generator
     * @throws JsonProcessingException thrown by JSON methods
     */
    public void toJson(JsonGenerator jsonGenerator) throws IOException, JsonProcessingException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("jipipe:graph-compartment", StringUtils.nullToEmpty(getCompartmentUUIDInGraph()));
        jsonGenerator.writeStringField("jipipe:alias-id", StringUtils.nullToEmpty(getAliasIdInGraph()));
        jsonGenerator.writeObjectField("jipipe:slot-configuration", slotConfiguration);
        jsonGenerator.writeFieldName("jipipe:ui-grid-location");
        jsonGenerator.writeStartObject();
        for (Map.Entry<String, Map<String, Point>> visualModeEntry : locations.entrySet()) {
            if (visualModeEntry.getKey() == null)
                continue;
            jsonGenerator.writeFieldName(visualModeEntry.getKey());
            jsonGenerator.writeStartObject();
            for (Map.Entry<String, Point> entry : visualModeEntry.getValue().entrySet()) {
                if (entry.getKey() == null)
                    continue;
                jsonGenerator.writeFieldName(entry.getKey());
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField("x", entry.getValue().x);
                jsonGenerator.writeNumberField("y", entry.getValue().y);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.writeStringField("jipipe:node-info-id", getInfo().getId());

        ParameterUtils.serializeParametersToJson(this, jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    /**
     * Loads this algorithm from JSON
     *
     * @param node   The JSON data to load from
     * @param issues issues during deserializing
     */
    public void fromJson(JsonNode node, JIPipeIssueReport issues) {
        if (node.has("jipipe:slot-configuration"))
            slotConfiguration.fromJson(node.get("jipipe:slot-configuration"));
        if (node.has("jipipe:ui-grid-location")) {
            for (Map.Entry<String, JsonNode> visualModeEntry : ImmutableList.copyOf(node.get("jipipe:ui-grid-location").fields())) {
                String compartment = visualModeEntry.getKey();
                for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(visualModeEntry.getValue().fields())) {
                    JsonNode xValue = entry.getValue().path("x");
                    JsonNode yValue = entry.getValue().path("y");
                    if (!xValue.isMissingNode() && !yValue.isMissingNode()) {
                        setLocationWithin(compartment, new Point(xValue.asInt(), yValue.asInt()), entry.getKey());
                    }
                }
            }
        }

        // Deserialize algorithm-specific parameters
        ParameterUtils.deserializeParametersFromJson(this, node, issues);
    }

    /**
     * The storage path is used in {@link JIPipeRun} to indicate where output data is written
     * This is only used internally
     *
     * @return Storage path
     */
    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * Sets the storage path. Used by {@link JIPipeRun}
     *
     * @param storagePath Storage path
     */
    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Returns the internal storage path relative to the output folder.
     * Used internally by {@link JIPipeRun}
     *
     * @return Storage path relative to the output folder
     */
    public Path getInternalStoragePath() {
        return internalStoragePath;
    }

    /**
     * Sets the internal storage path relative to the output folder.
     * Used internally by {@link JIPipeRun}
     *
     * @param internalStoragePath Path relative to the output folder
     */
    public void setInternalStoragePath(Path internalStoragePath) {
        this.internalStoragePath = internalStoragePath;
    }

    /**
     * Returns the {@link JIPipeNodeInfo} that describes this algorithm
     *
     * @return The info
     */
    public JIPipeNodeInfo getInfo() {
        return info;
    }

    /**
     * Sets the {@link JIPipeNodeInfo} that describes this algorithm.
     * Please note that this function can be very dangerous and break everything.
     * This will affect how JIPipe (and especially the UI) handles this algorithm.
     * A use case is to convert algorithms from internal representations to their public variants (e.g. {@link org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput}
     * to {@link org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm}
     *
     * @param info the info
     */
    public void setInfo(JIPipeNodeInfo info) {
        this.info = info;
    }

    /**
     * Returns the output slot with the specified name.
     * Throws {@link NullPointerException} if the slot does not exist and {@link IllegalArgumentException} if the slot is not an output
     *
     * @param name Slot name
     * @return Slot instance
     */
    public JIPipeDataSlot getOutputSlot(String name) {
        JIPipeDataSlot slot = outputSlotMap.get(name);
        if (!slot.isOutput())
            throw new IllegalArgumentException("The slot " + name + " is not an output slot!");
        return slot;
    }

    /**
     * Returns the input slot with the specified name.
     * Throws {@link NullPointerException} if the slot does not exist and {@link IllegalArgumentException} if the slot is not an input
     *
     * @param name Slot name
     * @return Slot instance
     */
    public JIPipeDataSlot getInputSlot(String name) {
        JIPipeDataSlot slot = inputSlotMap.get(name);
        if (!slot.isInput())
            throw new IllegalArgumentException("The slot " + name + " is not an input slot!");
        return slot;
    }

    /**
     * Returns all input slots that do not have data set.
     *
     * @return List of slots
     */
    public List<JIPipeDataSlot> getOpenInputSlots() {
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            if (graph.getSourceSlots(inputSlot).isEmpty()) {
                result.add(inputSlot);
            }
        }
        return result;
    }

    /**
     * Returns the first output slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no output slot.
     *
     * @return Slot instance
     */
    public JIPipeDataSlot getFirstOutputSlot() {
        return getOutputSlots().get(0);
    }

    /**
     * Returns the first input slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no input slot.
     *
     * @return Slot instance
     */
    public JIPipeDataSlot getFirstInputSlot() {
        return getInputSlots().get(0);
    }

    /**
     * Returns the last output slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no output slot.
     *
     * @return Slot instance
     */
    public JIPipeDataSlot getLastOutputSlot() {
        List<JIPipeDataSlot> outputSlots = getOutputSlots();
        return outputSlots.get(outputSlots.size() - 1);
    }

    /**
     * Returns the last input slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no input slot.
     *
     * @return Slot instance
     */
    public JIPipeDataSlot getLastInputSlot() {
        List<JIPipeDataSlot> inputSlots = getInputSlots();
        return inputSlots.get(inputSlots.size() - 1);
    }

    /**
     * Returns the graph this algorithm is located in.
     * Can be null.
     *
     * @return Graph instance or null
     */
    public JIPipeGraph getGraph() {
        return graph;
    }

    /**
     * Sets the graph this algorithm is location in.
     * This has no side effects and is only for reference usage.
     *
     * @param graph Graph instance or null
     */
    public void setGraph(JIPipeGraph graph) {
        this.graph = graph;
    }

    /**
     * Returns the UUID within the current graph. Requires that getGraph() is not null.
     *
     * @return The UUID within getGraph()
     */
    public UUID getUUIDInGraph() {
        return graph.getUUIDOf(this);
    }

    /**
     * Returns the compartment UUID within the current graph. Requires that getGraph() is not null.
     *
     * @return The UUID within getGraph()
     */
    public UUID getCompartmentUUIDInGraph() {
        if (graph != null) {
            return graph.getCompartmentUUIDOf(this);
        } else {
            return null;
        }
    }

    /**
     * Removes all location information added via setLocationWithin()
     */
    public void clearLocations() {
        locations.clear();
    }

    /**
     * Returns the custom description that is set by the user
     *
     * @return Description or null
     */
    @JIPipeDocumentation(name = "Description", description = "A custom description")
    @StringParameterSettings(multiline = true)
    @JIPipeParameter(value = "jipipe:node:description", uiOrder = -999)
    public HTMLText getCustomDescription() {
        if (customDescription == null)
            customDescription = new HTMLText();
        return customDescription;
    }

    /**
     * Sets a custom description. Can be null.
     *
     * @param customDescription Description string
     */
    @JIPipeParameter("jipipe:node:description")
    public void setCustomDescription(HTMLText customDescription) {
        this.customDescription = customDescription;
    }

    /**
     * Returns the current work directory of this algorithm. This is used internally to allow relative data paths.
     *
     * @return The current work directory or null.
     */
    public Path getProjectWorkDirectory() {
        return projectWorkDirectory;
    }

    /**
     * Sets the current work directory of this algorithm. This is used internally to allow loading data from relative paths.
     * This triggers a {@link JIPipeProject.WorkDirectoryChangedEvent} that can be received by {@link JIPipeDataSlot} instances to adapt to the work directory.
     *
     * @param projectWorkDirectory The work directory. Can be null
     */
    public void setProjectWorkDirectory(Path projectWorkDirectory) {
        this.projectWorkDirectory = projectWorkDirectory;
        eventBus.post(new JIPipeProject.WorkDirectoryChangedEvent(projectWorkDirectory));
    }

    /**
     * Gets the scratch directory of the current run associated to this algorithm.
     *
     * @return the scratch base directory
     */
    public Path getScratchBaseDirectory() {
        return scratchBaseDirectory;
    }

    /**
     * Sets the scratch base directory of the current run.
     * Please note that this directory will be propagated to sub-graphs
     *
     * @param scratchBaseDirectory the scratch base directory
     */
    public void setScratchBaseDirectory(Path scratchBaseDirectory) {
        this.scratchBaseDirectory = scratchBaseDirectory;
    }

    /**
     * Returns a new scratch directory that is unique and based on the alias ID of this node
     *
     * @return the scratch directory
     */
    public Path getNewScratch() {
        if (getScratchBaseDirectory() == null) {
            return RuntimeSettings.generateTempDirectory(getGraph() != null ? getAliasIdInGraph() : "scratch");
        }
        try {
            return Files.createTempDirectory(getScratchBaseDirectory(), getGraph() != null ? getAliasIdInGraph() : "scratch");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called by an {@link JIPipeGraph} when a slot was connected. Triggers update of slot inheritance.
     *
     * @param event The event generated by the graph
     */
    public void onSlotConnected(JIPipeGraph.NodeConnectedEvent event) {
        updateSlotInheritance();
    }

    private Class<? extends JIPipeData> getExpectedSlotDataType(JIPipeDataSlotInfo slotDefinition, JIPipeDataSlot slotInstance) {
        String inheritedSlotName = slotDefinition.getInheritedSlot();
        if (inheritedSlotName == null || inheritedSlotName.isEmpty())
            return slotInstance.getAcceptedDataType();
        if ("*".equals(inheritedSlotName) && !getInputSlots().isEmpty()) {
            inheritedSlotName = getInputSlotOrder().get(0);
        }

        JIPipeDataSlot inheritedSlot = inputSlotMap.getOrDefault(inheritedSlotName, null);
        if (inheritedSlot == null || inheritedSlot.getSlotType() != JIPipeSlotType.Input)
            return slotInstance.getAcceptedDataType();

        // Inherit from the inherited slot if there is no graph connection
        Set<JIPipeDataSlot> sourceSlots = graph.getSourceSlots(inheritedSlot);
        Class<? extends JIPipeData> inheritedType;
        if (sourceSlots.isEmpty())
            inheritedType = JIPipeDataSlotInfo.applyInheritanceConversion(slotDefinition, inheritedSlot.getAcceptedDataType());
        else
            inheritedType = JIPipeDataSlotInfo.applyInheritanceConversion(slotDefinition, sourceSlots.iterator().next().getAcceptedDataType());

        // Check if the inherited type is even compatible with the actual type
        if (slotDefinition.getDataClass().isAssignableFrom(inheritedType))
            return inheritedType;
        else
            return slotDefinition.getDataClass();
    }

    /**
     * Updates the inheritance: Passes traits from input slots to output slots while modifying the results depending on
     * the trait configuration.
     */
    public void updateSlotInheritance() {
        if (graph != null) {
            boolean modified = false;
            for (Map.Entry<String, JIPipeDataSlotInfo> entry : getSlotConfiguration().getOutputSlots().entrySet()) {
                JIPipeDataSlot slotInstance = outputSlotMap.getOrDefault(entry.getKey(), null);
                if (slotInstance == null || slotInstance.getSlotType() != JIPipeSlotType.Output)
                    continue;

                Class<? extends JIPipeData> expectedSlotDataType = getExpectedSlotDataType(entry.getValue(), slotInstance);
                if (slotInstance.getAcceptedDataType() != expectedSlotDataType) {
                    slotInstance.setAcceptedDataType(expectedSlotDataType);
                    triggerSlotsChangedEvent();
                    modified = true;
                }
            }
            if (modified) {
                Set<JIPipeGraphNode> algorithms = new HashSet<>();
                for (JIPipeDataSlot slot : getOutputSlots()) {
                    for (JIPipeDataSlot targetSlot : graph.getTargetSlots(slot)) {
                        algorithms.add(targetSlot.getNode());
                    }
                }
                for (JIPipeGraphNode algorithm : algorithms) {
                    algorithm.updateSlotInheritance();
                }
            }
        }
    }

    /**
     * Triggers an event that indicates that the slots have changed
     */
    public void triggerSlotsChangedEvent() {
        eventBus.post(new JIPipeGraph.NodeSlotsChangedEvent(this));
    }

    /**
     * Called by the {@link JIPipeGraph} to trigger slot inheritance updates when a slot is disconnected
     *
     * @param event The generated event
     */
    public void onSlotDisconnected(JIPipeGraph.NodeDisconnectedEvent event) {
        updateSlotInheritance();
    }

    /**
     * Returns a list of all dependencies this algorithm currently has.
     *
     * @return List of dependencies
     */
    public Set<JIPipeDependency> getDependencies() {
        Set<JIPipeDependency> result = new HashSet<>();
        result.add(JIPipe.getNodes().getSourceOf(getInfo().getId()));

        // Add data slots
        for (JIPipeDataSlot slot : inputSlots) {
            result.add(JIPipe.getDataTypes().getSourceOf(slot.getAcceptedDataType()));
        }
        for (JIPipeDataSlot slot : outputSlots) {
            result.add(JIPipe.getDataTypes().getSourceOf(slot.getAcceptedDataType()));
        }

        return result;
    }

    /**
     * Registers a sub-parameter instance to pass {@link ParameterStructureChangedEvent} via this algorithm's {@link EventBus}
     *
     * @param subParameter the sub-parameter
     */
    protected void registerSubParameter(JIPipeParameterCollection subParameter) {
        subParameter.getEventBus().register(this);
    }

    /**
     * Triggered when the parameter structure of this algorithm was changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        if (event.getVisitors().contains(this))
            return;
        event.getVisitors().add(this);
        getEventBus().post(event);
    }

    /**
     * Triggered when the parameter UI structure of this algorithm was changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterUIChanged(ParameterUIChangedEvent event) {
        if (event.getVisitors().contains(this))
            return;
        event.getVisitors().add(this);
        getEventBus().post(event);
    }

    /**
     * Clears all data slots
     */
    public void clearSlotData() {
        for (JIPipeDataSlot slot : inputSlots) {
            slot.clearData();
        }
        for (JIPipeDataSlot slot : outputSlots) {
            slot.clearData();
        }
    }

    /**
     * Copies the algorithm.
     * Produces a deep copy
     *
     * @return the copy
     */
    public JIPipeGraphNode duplicate() {
        return getInfo().duplicate(this);
    }

    /**
     * Returns true if a user can delete this algorithm
     *
     * @return if a user can delete this algorithm
     */
    public boolean canUserDelete() {
        return graph.canUserDelete(this);
    }

    /**
     * Returns true if the algorithm contains an input slot with given name.
     * Returns false if there is an output slot with given name
     *
     * @param slotName the slot name
     * @return if the algorithm contains an input slot with given name.
     */
    public boolean hasInputSlot(String slotName) {
        JIPipeDataSlot existing = inputSlotMap.getOrDefault(slotName, null);
        return existing != null && existing.isInput();
    }

    /**
     * Returns true if the algorithm contains an output slot with given name.
     * Returns false if there is an input slot with given name
     *
     * @param slotName the slot name
     * @return if the algorithm contains an output slot with given name.
     */
    public boolean hasOutputSlot(String slotName) {
        JIPipeDataSlot existing = outputSlotMap.getOrDefault(slotName, null);
        return existing != null && existing.isOutput();
    }

    /**
     * Controls whether to render input slots.
     * If disabled, this achieves the same effect as a compartment output within a foreign compartment.
     *
     * @return render input slots
     */
    public boolean renderInputSlots() {
        return true;
    }

    /**
     * Controls whether to render output slots.
     * If disabled, this achieves the same effect as a compartment output within the same compartment.
     *
     * @return render output slots
     */
    public boolean renderOutputSlots() {
        return true;
    }

    /**
     * Sets/unsets virtual mode for all output slots
     *
     * @param virtual      virtual mode
     * @param apply        apply the setting
     * @param progressInfo progress for applying. Can be null if apply is false.
     */
    public void setAllSlotsVirtual(boolean virtual, boolean apply, JIPipeProgressInfo progressInfo) {
        for (JIPipeDataSlot slot : getInputSlots()) {
            slot.getInfo().setVirtual(virtual);
            if (apply)
                slot.applyVirtualState(progressInfo);
        }
        for (JIPipeDataSlot slot : getOutputSlots()) {
            slot.getInfo().setVirtual(virtual);
            if (apply)
                slot.applyVirtualState(progressInfo);
        }
    }

    public boolean isVisibleIn(UUID compartmentUUIDInGraph) {
        UUID currentCompartmentUUID = getCompartmentUUIDInGraph();
        if (Objects.equals(compartmentUUIDInGraph, currentCompartmentUUID))
            return true;
        return graph.getVisibleCompartmentUUIDsOf(this).contains(compartmentUUIDInGraph);
    }

    /**
     * Returns a display name for the compartment
     *
     * @return the display name for the compartment
     */
    public String getCompartmentDisplayName() {
        String compartment = "";
        if (graph != null) {
            UUID compartmentUUID = getCompartmentUUIDInGraph();
            if (compartmentUUID != null) {
                JIPipeProject project = graph.getProject();
                if (project != null) {
                    JIPipeProjectCompartment projectCompartment = project.getCompartments().getOrDefault(compartmentUUID, null);
                    if (projectCompartment != null) {
                        compartment = projectCompartment.getName();
                    }
                }
                if (compartment == null)
                    compartment = compartmentUUID.toString();
            }
        }
        return compartment;
    }

    /**
     * Returns a name that provides human-readable information about the name and compartment
     *
     * @return the display name
     */
    public String getDisplayName() {
        String compartment = getCompartmentDisplayName();
        return getName() + (!StringUtils.isNullOrEmpty(compartment) ? " in compartment '" + compartment + "'" : "");
    }

    /**
     * Returns the compartment UUID in the graph as string.
     * If the UUID is null, an empty string is returned.
     *
     * @return UUID as string or an empty string of the compartment is null;
     */
    public String getCompartmentUUIDInGraphAsString() {
        return StringUtils.nullToEmpty(getCompartmentUUIDInGraph());
    }

    /**
     * Returns the alias ID of this node.
     * It is unique within the same graph, but should not be used to identify it due to dependency on the node's name.
     * Use the UUID instead.
     *
     * @return the alias ID. Null if there is no graph.
     */
    public String getAliasIdInGraph() {
        if (graph != null) {
            return graph.getAliasIdOf(this);
        } else {
            return null;
        }
    }

    /**
     * Gets the project compartment instance of this node.
     * Returns null if there is no project.
     *
     * @return the compartment
     */
    public JIPipeProjectCompartment getProjectCompartment() {
        JIPipeProject project = graph.getAttachment(JIPipeProject.class);
        return project.getCompartments().getOrDefault(getCompartmentUUIDInGraph(), null);
    }

    /**
     * Toggles a slot with given info on/off. Requires that the slot configuration is a {@link org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration}
     *
     * @param info   the slot info
     * @param toggle if the slot should be present
     */
    public void toggleSlot(JIPipeDataSlotInfo info, boolean toggle) {
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        if (toggle) {
            if (info.getSlotType() == JIPipeSlotType.Input) {
                if (!getInputSlotMap().containsKey(info.getName())) {
                    slotConfiguration.addSlot(info.getName(), info, false);
                }
            } else {
                if (!getOutputSlotMap().containsKey(info.getName())) {
                    slotConfiguration.addSlot(info.getName(), info, false);
                }
            }
        } else {
            if (info.getSlotType() == JIPipeSlotType.Input) {
                if (getInputSlotMap().containsKey(info.getName())) {
                    slotConfiguration.removeInputSlot(info.getName(), false);
                }
            } else {
                if (getOutputSlotMap().containsKey(info.getName())) {
                    slotConfiguration.removeOutputSlot(info.getName(), false);
                }
            }
        }
    }

    /**
     * Serializes an {@link JIPipeGraphNode} instance
     */
    public static class Serializer extends JsonSerializer<JIPipeGraphNode> {
        @Override
        public void serialize(JIPipeGraphNode algorithm, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            algorithm.toJson(jsonGenerator);
        }
    }

}
