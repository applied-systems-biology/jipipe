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

package org.hkijena.pipelinej.api.algorithm;

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
import org.hkijena.pipelinej.ACAQDependency;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQRun;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidatable;
import org.hkijena.pipelinej.api.data.*;
import org.hkijena.pipelinej.api.events.*;
import org.hkijena.pipelinej.api.parameters.*;
import org.hkijena.pipelinej.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.pipelinej.api.registries.ACAQDatatypeRegistry;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.pipelinej.utils.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An algorithm is is a set of input and output data slots, and a run() function
 * It is part of the {@link ACAQGraph}.
 * <p>
 * The workload of this node is considered optional (this means it can have an empty workload),
 * which is important for structural graphs like the compartment or trait graph.
 * Use {@link ACAQAlgorithm} as base class to indicate a non-optional workload.
 */
@JsonSerialize(using = ACAQGraphNode.Serializer.class)
public abstract class ACAQGraphNode implements ACAQValidatable, ACAQParameterCollection {
    private ACAQAlgorithmDeclaration declaration;
    private ACAQSlotConfiguration slotConfiguration;
    private List<ACAQDataSlot> inputSlots = new ArrayList<>();
    private List<ACAQDataSlot> outputSlots = new ArrayList<>();
    private BiMap<String, ACAQDataSlot> inputSlotMap = HashBiMap.create();
    private BiMap<String, ACAQDataSlot> outputSlotMap = HashBiMap.create();
    private EventBus eventBus = new EventBus();
    private Map<String, Map<String, Point>> locations = new HashMap<>();
    private Path internalStoragePath;
    private Path storagePath;
    private String customName;
    private String customDescription;
    private String compartment;
    private Set<String> visibleCompartments = new HashSet<>();
    private ACAQGraph graph;
    private Path workDirectory;

    /**
     * Initializes this algorithm with a custom provided slot configuration and trait configuration
     *
     * @param declaration       Contains algorithm metadata
     * @param slotConfiguration if null, generate the slot configuration
     */
    public ACAQGraphNode(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        this.declaration = declaration;
        if (slotConfiguration == null) {
            ACAQDefaultMutableSlotConfiguration.Builder builder = ACAQDefaultMutableSlotConfiguration.builder();

            for (AlgorithmInputSlot slot : getClass().getAnnotationsByType(AlgorithmInputSlot.class)) {
                if (slot.autoCreate()) {
                    builder.addInputSlot(slot.slotName(), slot.value());
                }
            }
            for (AlgorithmOutputSlot slot : getClass().getAnnotationsByType(AlgorithmOutputSlot.class)) {
                if (slot.autoCreate()) {
                    if (!slot.inheritedSlot().isEmpty())
                        builder.allowOutputSlotInheritance(true);
                    builder.addOutputSlot(slot.slotName(), slot.value(), slot.inheritedSlot());
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
     * Initializes a new algorithm instance
     *
     * @param declaration The algorithm declaration
     */
    public ACAQGraphNode(ACAQAlgorithmDeclaration declaration) {
        this(declaration, null);
    }

    /**
     * Copies the input algorithm's properties into this algorithm
     *
     * @param other Copied algorithm
     */
    public ACAQGraphNode(ACAQGraphNode other) {
        this.declaration = other.declaration;
        this.slotConfiguration = copySlotConfiguration(other);
        this.locations = new HashMap<>();
        for (Map.Entry<String, Map<String, Point>> entry : other.locations.entrySet()) {
            locations.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        this.compartment = other.compartment;
        this.visibleCompartments = new HashSet<>(other.visibleCompartments);
        this.customName = other.customName;
        this.customDescription = other.customDescription;
        updateGraphNodeSlots();
        slotConfiguration.getEventBus().register(this);
    }

    /**
     * Synchronizes the slots with the slot definition
     */
    public void updateGraphNodeSlots() {
        boolean changed = false;

        // This loop handles add & replace
        for (Map.Entry<String, ACAQSlotDefinition> entry : slotConfiguration.getInputSlots().entrySet()) {
            changed |= updateAddAndModifySlots(changed, entry, inputSlotMap, inputSlots);
        }
        for (Map.Entry<String, ACAQSlotDefinition> entry : slotConfiguration.getOutputSlots().entrySet()) {
            changed |= updateAddAndModifySlots(changed, entry, outputSlotMap, outputSlots);
        }

        // This loop handles remove
        changed |= updateRemoveSlots(changed, slotConfiguration.getInputSlots(), inputSlotMap, inputSlots);
        changed |= updateRemoveSlots(changed, slotConfiguration.getOutputSlots(), outputSlotMap, outputSlots);

        // Update according to slot order
        changed |= updateSlotOrder(slotConfiguration.getInputSlotOrder(), inputSlotMap, inputSlots);
        changed |= updateSlotOrder(slotConfiguration.getOutputSlotOrder(), outputSlotMap, outputSlots);

        if (changed) {
            eventBus.post(new AlgorithmSlotsChangedEvent(this));
            updateSlotInheritance();
        }
    }

    private boolean updateSlotOrder(List<String> configurationOrder, BiMap<String, ACAQDataSlot> slotMap, List<ACAQDataSlot> slots) {
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
            ACAQDataSlot instance = slotMap.getOrDefault(name, null);
            if (instance != null) {
                slots.add(instance);
            }
        }
        for (ACAQDataSlot instance : slotMap.values()) {
            if (!slots.contains(instance)) {
                slots.add(instance);
            }
        }
        return true;
    }

    private boolean updateRemoveSlots(boolean changed, Map<String, ACAQSlotDefinition> definedSlots, BiMap<String, ACAQDataSlot> slotMap, List<ACAQDataSlot> slots) {
        for (String name : ImmutableList.copyOf(slotMap.keySet())) {
            if (!definedSlots.containsKey(name)) {
                ACAQDataSlot existing = slotMap.get(name);
                slotMap.remove(name);
                slots.remove(existing);
                changed = true;
            }
        }
        return changed;
    }

    private boolean updateAddAndModifySlots(boolean changed, Map.Entry<String, ACAQSlotDefinition> entry, BiMap<String, ACAQDataSlot> slotMap, List<ACAQDataSlot> slots) {
        ACAQDataSlot existing = slotMap.getOrDefault(entry.getKey(), null);
        if (existing != null) {
            if (!Objects.equals(existing.getDefinition(), entry.getValue())) {
                slotMap.remove(entry.getKey());
                slots.remove(existing);
                existing = null;
            }
        }
        if (existing == null) {
            existing = new ACAQDataSlot(entry.getValue(), this);
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
    protected ACAQSlotConfiguration copySlotConfiguration(ACAQGraphNode other) {
        ACAQDefaultMutableSlotConfiguration configuration = ACAQDefaultMutableSlotConfiguration.builder().build();
        configuration.setTo(other.slotConfiguration);
        return configuration;
    }

    /**
     * Runs the workload
     *
     * @param subProgress       allows algorithms to report sub-progress
     * @param algorithmProgress call to provide sub-progress
     * @param isCancelled       returns if cancellation was requested
     */
    public abstract void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled);

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the algorithm name
     *
     * @return algorithm name
     */
    @ACAQParameter(value = "acaq:node:name", visibility = ACAQParameterVisibility.Visible, uiOrder = -9999)
    @ACAQDocumentation(name = "Name", description = "Custom algorithm name.")
    public String getName() {
        if (customName == null || customName.isEmpty())
            return getDeclaration().getName();
        return customName;
    }

    /**
     * Sets a custom name. If set to null, the standard algorithm name is automatically used by getName()
     *
     * @param customName custom name
     */
    @ACAQParameter("acaq:node:name")
    public void setCustomName(String customName) {
        this.customName = customName;

    }

    /**
     * Gets the algorithm category
     *
     * @return The category
     */
    public ACAQAlgorithmCategory getCategory() {
        return getDeclaration().getCategory();
    }

    /**
     * Gets the slot configuration
     *
     * @return Slot configuration
     */
    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public void setSlotConfiguration(ACAQSlotConfiguration slotConfiguration) {
        this.slotConfiguration = slotConfiguration;
    }

    /**
     * Gets all input slot instances
     *
     * @return Current algorithm slots
     */
    public Map<String, ACAQDataSlot> getInputSlotMap() {
        return Collections.unmodifiableMap(inputSlotMap);
    }

    /**
     * Gets all output slot instances
     *
     * @return Current algorithm slots
     */
    public Map<String, ACAQDataSlot> getOutputSlotMap() {
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
    public List<ACAQDataSlot> getInputSlots() {
        return Collections.unmodifiableList(inputSlots);
    }

    /**
     * Returns all output slots ordered by the slot order
     *
     * @return List of slots
     */
    public List<ACAQDataSlot> getOutputSlots() {
        return Collections.unmodifiableList(outputSlots);
    }

    /**
     * Triggered when a parameter is changed within the slot configuration.
     * Triggers a {@link AlgorithmSlotsChangedEvent}
     *
     * @param event generated event
     */
    @Subscribe
    public void onSlotConfigurationParameterChanged(ParameterChangedEvent event) {
        if (event.getSource() == slotConfiguration) {
            getEventBus().post(new AlgorithmSlotsChangedEvent(this));
        }
    }

    /**
     * Should be triggered when a slot is added to the slot configuration
     *
     * @param event The event
     */
    @Subscribe
    public void onSlotConfigurationChanged(SlotsChangedEvent event) {
        if (event.getConfiguration() == getSlotConfiguration()) {
            updateGraphNodeSlots();
        }
    }

    public Map<String, Map<String, Point>> getLocations() {
        return locations;
    }

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
     * Saves this algorithm to JSON.
     * Override this method to apply your own modifications
     *
     * @param jsonGenerator the JSON generator
     * @throws JsonProcessingException thrown by JSON methods
     */
    public void toJson(JsonGenerator jsonGenerator) throws IOException, JsonProcessingException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("acaq:slot-configuration", slotConfiguration);
        jsonGenerator.writeFieldName("acaq:algorithm-ui-location");
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
        jsonGenerator.writeStringField("acaq:algorithm-type", getDeclaration().getId());
        jsonGenerator.writeStringField("acaq:algorithm-compartment", getCompartment());
        ACAQParameterTree parameterCollection = new ACAQParameterTree(this);
        for (Map.Entry<String, ACAQParameterAccess> entry : parameterCollection.getParameters().entrySet()) {
            if (entry.getValue().isPersistent()) {
                jsonGenerator.writeObjectField(entry.getKey(), entry.getValue().get(Object.class));
            }
        }

        // Save dynamic parameter storage
        Set<ACAQParameterCollection> dynamicParameters = parameterCollection.getRegisteredSources().stream()
                .filter(src -> src instanceof ACAQDynamicParameterCollection).collect(Collectors.toSet());
        if (!dynamicParameters.isEmpty()) {
            jsonGenerator.writeFieldName("acaq:dynamic-parameters");
            jsonGenerator.writeStartObject();
            for (ACAQParameterCollection dynamicParameter : dynamicParameters) {
                String key = parameterCollection.getSourceKey(dynamicParameter);
                jsonGenerator.writeObjectField(key, dynamicParameter);
            }
            jsonGenerator.writeEndObject();
        }

        jsonGenerator.writeEndObject();
    }

    /**
     * Loads this algorithm from JSON
     *
     * @param node The JSON data to load from
     */
    public void fromJson(JsonNode node) {

        // Load compartment
        compartment = node.get("acaq:algorithm-compartment").asText();

        if (node.has("acaq:slot-configuration"))
            slotConfiguration.fromJson(node.get("acaq:slot-configuration"));
        if (node.has("acaq:algorithm-ui-location")) {
            for (Map.Entry<String, JsonNode> visualModeEntry : ImmutableList.copyOf(node.get("acaq:algorithm-ui-location").fields())) {
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

        // Deserialize dynamic parameters
        if (node.has("acaq:dynamic-parameters")) {
            ACAQParameterTree parameterCollection = new ACAQParameterTree(this);
            Set<ACAQParameterCollection> dynamicParameters = parameterCollection.getRegisteredSources().stream()
                    .filter(src -> src instanceof ACAQDynamicParameterCollection).collect(Collectors.toSet());
            for (ACAQParameterCollection dynamicParameter : dynamicParameters) {
                String key = parameterCollection.getSourceKey(dynamicParameter);
                JsonNode entryNode = node.path("acaq:dynamic-parameters").path(key);
                if (!entryNode.isMissingNode()) {
                    ((ACAQDynamicParameterCollection) dynamicParameter).fromJson(entryNode);
                }
            }

        }

        // Deserialize algorithm-specific parameters
        ACAQParameterCollection.deserializeParametersFromJson(this, node);
    }

    /**
     * The storage path is used in {@link ACAQRun} to indicate where output data is written
     * This is only used internally
     *
     * @return Storage path
     */
    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * Sets the storage path. Used by {@link ACAQRun}
     *
     * @param storagePath Storage path
     */
    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Returns the internal storage path relative to the output folder.
     * Used internally by {@link ACAQRun}
     *
     * @return Storage path relative to the output folder
     */
    public Path getInternalStoragePath() {
        return internalStoragePath;
    }

    /**
     * Sets the internal storage path relative to the output folder.
     * Used internally by {@link ACAQRun}
     *
     * @param internalStoragePath Path relative to the output folder
     */
    public void setInternalStoragePath(Path internalStoragePath) {
        this.internalStoragePath = internalStoragePath;
    }

    /**
     * Returns the {@link ACAQAlgorithmDeclaration} that describes this algorithm
     *
     * @return The declaration
     */
    public ACAQAlgorithmDeclaration getDeclaration() {
        return declaration;
    }

    /**
     * Sets the {@link ACAQAlgorithmDeclaration} that describes this algorithm.
     * Please note that this function can be very dangerous and break everything.
     * This will affect how ACAQ5 (and especially the UI) handles this algorithm.
     * A use case is to convert algorithms from internal representations to their public variants (e.g. {@link org.hkijena.pipelinej.api.compartments.algorithms.ACAQCompartmentOutput}
     * to {@link org.hkijena.pipelinej.api.compartments.algorithms.IOInterfaceAlgorithm}
     *
     * @param declaration
     */
    public void setDeclaration(ACAQAlgorithmDeclaration declaration) {
        this.declaration = declaration;
    }

    /**
     * Returns the compartment the algorithm is located within
     *
     * @return Compartment ID
     */
    public String getCompartment() {
        return compartment;
    }

    /**
     * Sets the compartment the algorithm is location in
     *
     * @param compartment Compartment ID
     */
    public void setCompartment(String compartment) {
        this.compartment = compartment;
    }

    /**
     * Returns true if this algorithm is visible in the specified container compartment
     *
     * @param containerCompartment The compartment ID the container displays
     * @return If this algorithm should be visible
     */
    public boolean isVisibleIn(String containerCompartment) {
        return StringUtils.isNullOrEmpty(compartment) || StringUtils.isNullOrEmpty(containerCompartment) ||
                containerCompartment.equals(compartment) || visibleCompartments.contains(containerCompartment);
    }

    /**
     * Returns the list of additional compartments this algorithm is visible in.
     * This list is writable.
     *
     * @return Writeable list of project compartment IDs
     */
    public Set<String> getVisibleCompartments() {
        return visibleCompartments;
    }

    /**
     * Sets the list of additional compartments this algorithm is visible in.
     *
     * @param visibleCompartments List of compartment Ids
     */
    public void setVisibleCompartments(Set<String> visibleCompartments) {
        this.visibleCompartments = visibleCompartments;
    }

    /**
     * Returns the output slot with the specified name.
     * Throws {@link NullPointerException} if the slot does not exist and {@link IllegalArgumentException} if the slot is not an output
     *
     * @param name Slot name
     * @return Slot instance
     */
    public ACAQDataSlot getOutputSlot(String name) {
        ACAQDataSlot slot = outputSlotMap.get(name);
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
    public ACAQDataSlot getInputSlot(String name) {
        ACAQDataSlot slot = inputSlotMap.get(name);
        if (!slot.isInput())
            throw new IllegalArgumentException("The slot " + name + " is not an input slot!");
        return slot;
    }

    /**
     * Returns all input slots that do not have data set.
     *
     * @return List of slots
     */
    public List<ACAQDataSlot> getOpenInputSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            if (graph.getSourceSlot(inputSlot) == null) {
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
    public ACAQDataSlot getFirstOutputSlot() {
        return getOutputSlots().get(0);
    }

    /**
     * Returns the first input slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no input slot.
     *
     * @return Slot instance
     */
    public ACAQDataSlot getFirstInputSlot() {
        return getInputSlots().get(0);
    }

    /**
     * Returns the last output slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no output slot.
     *
     * @return Slot instance
     */
    public ACAQDataSlot getLastOutputSlot() {
        List<ACAQDataSlot> outputSlots = getOutputSlots();
        return outputSlots.get(outputSlots.size() - 1);
    }

    /**
     * Returns the last input slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no input slot.
     *
     * @return Slot instance
     */
    public ACAQDataSlot getLastInputSlot() {
        List<ACAQDataSlot> inputSlots = getInputSlots();
        return inputSlots.get(inputSlots.size() - 1);
    }

    /**
     * Returns the graph this algorithm is located in.
     * Can be null.
     *
     * @return Graph instance or null
     */
    public ACAQGraph getGraph() {
        return graph;
    }

    /**
     * Sets the graph this algorithm is location in.
     * This has no side effects and is only for reference usage.
     *
     * @param graph Graph instance or null
     */
    public void setGraph(ACAQGraph graph) {
        this.graph = graph;
    }

    /**
     * Returns the ID within the current graph. Requires that getGraph() is not null.
     *
     * @return The ID within getGraph()
     */
    public String getIdInGraph() {
        return graph.getIdOf(this);
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
    @ACAQDocumentation(name = "Description", description = "A custom description")
    @StringParameterSettings(multiline = true)
    @ACAQParameter(value = "acaq:node:description", visibility = ACAQParameterVisibility.Visible, uiOrder = -999)
    public String getCustomDescription() {
        return customDescription;
    }

    /**
     * Sets a custom description. Can be null.
     *
     * @param customDescription Description string
     */
    @ACAQParameter("acaq:node:description")
    public void setCustomDescription(String customDescription) {
        this.customDescription = customDescription;
    }

    /**
     * Returns the current work directory of this algorithm. This is used internally to allow relative data paths.
     *
     * @return The current work directory or null.
     */
    public Path getWorkDirectory() {
        return workDirectory;
    }

    /**
     * Sets the current work directory of this algorithm. This is used internally to allow loading data from relative paths.
     * This triggers a {@link WorkDirectoryChangedEvent} that can be received by {@link ACAQDataSlot} instances to adapt to the work directory.
     *
     * @param workDirectory The work directory. Can be null
     */
    public void setWorkDirectory(Path workDirectory) {
        this.workDirectory = workDirectory;
        eventBus.post(new WorkDirectoryChangedEvent(workDirectory));
    }

    /**
     * Called by an {@link ACAQGraph} when a slot was connected. Triggers update of slot trait inheritance.
     *
     * @param event The event generated by the graph
     */
    public void onSlotConnected(AlgorithmGraphConnectedEvent event) {
        updateSlotInheritance();
    }

    private Class<? extends ACAQData> getExpectedSlotDataType(ACAQSlotDefinition slotDefinition, ACAQDataSlot slotInstance) {
        String inheritedSlotName = slotDefinition.getInheritedSlot();
        if (inheritedSlotName == null || inheritedSlotName.isEmpty())
            return slotInstance.getAcceptedDataType();
        if ("*".equals(inheritedSlotName) && !getInputSlots().isEmpty()) {
            inheritedSlotName = getInputSlotOrder().get(0);
        }

        ACAQDataSlot inheritedSlot = inputSlotMap.getOrDefault(inheritedSlotName, null);
        if (inheritedSlot == null || inheritedSlot.getSlotType() != ACAQSlotType.Input)
            return slotInstance.getAcceptedDataType();

        // Inherit from the inherited slot if there is no graph connection
        ACAQDataSlot sourceSlot = graph.getSourceSlot(inheritedSlot);
        if (sourceSlot == null)
            return ACAQSlotDefinition.applyInheritanceConversion(slotDefinition, inheritedSlot.getAcceptedDataType());
        return ACAQSlotDefinition.applyInheritanceConversion(slotDefinition, sourceSlot.getAcceptedDataType());
    }

    /**
     * Updates the inheritance: Passes traits from input slots to output slots while modifying the results depending on
     * the trait configuration.
     */
    public void updateSlotInheritance() {
        if (graph != null) {
            boolean modified = false;
            for (Map.Entry<String, ACAQSlotDefinition> entry : getSlotConfiguration().getOutputSlots().entrySet()) {
                ACAQDataSlot slotInstance = outputSlotMap.getOrDefault(entry.getKey(), null);
                if (slotInstance == null || slotInstance.getSlotType() != ACAQSlotType.Output)
                    continue;

                Class<? extends ACAQData> expectedSlotDataType = getExpectedSlotDataType(entry.getValue(), slotInstance);
                if (slotInstance.getAcceptedDataType() != expectedSlotDataType) {
                    slotInstance.setAcceptedDataType(expectedSlotDataType);
                    eventBus.post(new AlgorithmSlotsChangedEvent(this));
                    modified = true;
                }
            }
            if (modified) {
                Set<ACAQGraphNode> algorithms = new HashSet<>();
                for (ACAQDataSlot slot : getOutputSlots()) {
                    for (ACAQDataSlot targetSlot : graph.getTargetSlots(slot)) {
                        algorithms.add(targetSlot.getNode());
                    }
                }
                for (ACAQGraphNode algorithm : algorithms) {
                    algorithm.updateSlotInheritance();
                }
            }
        }
    }

    /**
     * Called by the {@link ACAQGraph} to trigger slot inheritance updates when a slot is disconnected
     *
     * @param event The generated event
     */
    public void onSlotDisconnected(AlgorithmGraphDisconnectedEvent event) {
        updateSlotInheritance();
    }

    /**
     * Returns a list of all dependencies this algorithm currently has.
     *
     * @return List of dependencies
     */
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = new HashSet<>();
        result.add(ACAQAlgorithmRegistry.getInstance().getSourceOf(getDeclaration().getId()));

        // Add data slots
        for (ACAQDataSlot slot : inputSlots) {
            result.add(ACAQDatatypeRegistry.getInstance().getSourceOf(slot.getAcceptedDataType()));
        }
        for (ACAQDataSlot slot : outputSlots) {
            result.add(ACAQDatatypeRegistry.getInstance().getSourceOf(slot.getAcceptedDataType()));
        }

        return result;
    }

    /**
     * Registers a sub-parameter instance to pass {@link ParameterStructureChangedEvent} via this algorithm's {@link EventBus}
     *
     * @param subParameter the sub-parameter
     */
    protected void registerSubParameter(ACAQParameterCollection subParameter) {
        subParameter.getEventBus().register(this);
    }

    /**
     * Triggered when the parameter structure of this algorithm was changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        getEventBus().post(event);
    }

    /**
     * Clears all data slots
     */
    public void clearSlotData() {
        for (ACAQDataSlot slot : inputSlots) {
            slot.clearData();
        }
        for (ACAQDataSlot slot : outputSlots) {
            slot.clearData();
        }
    }

    /**
     * Copies the algorithm.
     * Produces a deep copy
     *
     * @return the copy
     */
    public ACAQGraphNode duplicate() {
        return getDeclaration().clone(this);
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
        ACAQDataSlot existing = inputSlotMap.getOrDefault(slotName, null);
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
        ACAQDataSlot existing = outputSlotMap.getOrDefault(slotName, null);
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
     * Utility function to create an algorithm instance from its id
     *
     * @param id  Algorithm ID
     * @param <T> Algorithm class
     * @return Algorithm instance
     */
    public static <T extends ACAQGraphNode> T newInstance(String id) {
        return (T) ACAQAlgorithmRegistry.getInstance().getDeclarationById(id).newInstance();
    }

    /**
     * Serializes an {@link ACAQGraphNode} instance
     */
    public static class Serializer extends JsonSerializer<ACAQGraphNode> {
        @Override
        public void serialize(ACAQGraphNode algorithm, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            algorithm.toJson(jsonGenerator);
        }
    }
}
