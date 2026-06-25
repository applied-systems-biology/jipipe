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
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeFunctionallyComparable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurationCache;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurator;
import org.hkijena.jipipe.api.servers.JIPipeServerInstance;
import org.hkijena.jipipe.api.servers.JIPipeServerLease;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.ui.ViewOnlyMenuItem;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A node is a set of input and output data slots, and a run() function
 * It is part of the {@link JIPipeGraph}.
 * The workload of this node is considered optional (this means it can have an empty workload),
 * which is important for structural graphs like the compartment graph.
 * Use {@link JIPipeAlgorithm} as base class to indicate a non-optional workload.
 */
@JsonSerialize(using = JIPipeGraphNode.Serializer.class)
public abstract class JIPipeGraphNode extends AbstractJIPipeParameterCollection implements JIPipeValidatable, JIPipeFunctionallyComparable, JIPipeParameterCollection.ParameterChangedEventListener,
        JIPipeParameterCollection.ParameterUIChangedEventListener, JIPipeParameterCollection.ParameterStructureChangedEventListener, JIPipeSlotConfiguration.SlotConfigurationChangedEventListener {

    private final NodeSlotsChangedEventEmitter nodeSlotsChangedEventEmitter = new NodeSlotsChangedEventEmitter();
    private final BaseDirectoryChangedEventEmitter baseDirectoryChangedEventEmitter = new BaseDirectoryChangedEventEmitter();

    private final List<JIPipeInputDataSlot> inputSlots = new ArrayList<>();
    private final List<JIPipeOutputDataSlot> outputSlots = new ArrayList<>();
    private final BiMap<String, JIPipeInputDataSlot> inputSlotMap = HashBiMap.create();
    private final BiMap<String, JIPipeOutputDataSlot> outputSlotMap = HashBiMap.create();
    private final JIPipeGraphNodeEnvironmentOverridesParameter environmentOverrides;
    private JIPipeNodeInfo info;
    private JIPipeSlotConfiguration slotConfiguration;
    private JIPipePathMetadataStore nodeMetadata = new JIPipePathMetadataStore();
    private Path internalStoragePath;
    private Path storagePath;
    private String customName;
    private HTMLText customDescription;
    private JIPipeGraph parentGraph;
    private JIPipeProject runtimeProject;
    private Path baseDirectory;
    private Path projectDirectory;
    private Path scratchBaseDirectory;
    private boolean bookmarked;
    private boolean uiLocked;


    /**
     * Initializes this algorithm with a custom provided slot configuration
     *
     * @param info              Contains algorithm metadata
     * @param slotConfiguration if null, generate the slot configuration automatically. Prefers the creation of slots via annotations. Alternatively, slots can also be created via the node info
     */
    public JIPipeGraphNode(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        this.info = info;
        this.environmentOverrides = new JIPipeGraphNodeEnvironmentOverridesParameter();
        registerSubParameter(environmentOverrides);
        if (slotConfiguration == null) {
            JIPipeDefaultMutableSlotConfiguration.Builder builder = JIPipeDefaultMutableSlotConfiguration.builder();
            initializeSlotsFromAnnotations(info, builder);
            builder.seal();
            slotConfiguration = builder.build();
        }

        this.slotConfiguration = slotConfiguration;
        slotConfiguration.getSlotConfigurationChangedEventEmitter().subscribe(this);
        updateEnvironmentOverrides();
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
        this.nodeMetadata = new JIPipePathMetadataStore(other.nodeMetadata);
        this.customName = other.customName;
        this.customDescription = other.customDescription;
        this.baseDirectory = other.baseDirectory;
        this.projectDirectory = other.projectDirectory;
        this.environmentOverrides = new JIPipeGraphNodeEnvironmentOverridesParameter(other.environmentOverrides);
        registerSubParameter(environmentOverrides);
        updateEnvironmentOverrides();
        updateGraphNodeSlots();
        slotConfiguration.getSlotConfigurationChangedEventEmitter().subscribe(this);
    }

    private void initializeSlotsFromAnnotations(JIPipeNodeInfo info, JIPipeDefaultMutableSlotConfiguration.Builder builder) {
        boolean created = false;
        for (AddJIPipeInputSlot slot : getClass().getAnnotationsByType(AddJIPipeInputSlot.class)) {
            if (slot.create()) {
                created = true;
                builder.addInputSlot(slot);
            }
        }
        for (AddJIPipeOutputSlot slot : getClass().getAnnotationsByType(AddJIPipeOutputSlot.class)) {
            if (slot.create()) {
                created = true;
                builder.addOutputSlot(slot);
            }
        }

        if (!created) {
            for (AddJIPipeInputSlot slot : info.getInputSlots()) {
                if (slot.create()) {
                    created = true;
                    builder.addInputSlot(slot);
                }
            }
            for (AddJIPipeOutputSlot slot : info.getOutputSlots()) {
                if (slot.create()) {
                    created = true;
                    builder.addOutputSlot(slot);
                }
            }
        }
    }

    /**
     * Update the list of environment override parameters
     */
    private void updateEnvironmentOverrides() {
        for (Class<? extends JIPipeEnvironment> environmentType : getRegisteredEnvironmentTypes()) {
            // Register parameter
            JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo = JIPipe.getInstance().getEnvironments().getInfoByClass(environmentType);
            if (!environmentOverrides.containsKey(environmentInfo.getId())) {
                environmentOverrides.addParameter(environmentInfo.getId(),
                        environmentInfo.getOptionalEnvironmentClass(),
                        environmentInfo.getName(),
                        "Allows to override the " + environmentInfo.getName() + " environment. " +
                                environmentInfo.getDescription());
            }
        }
    }

    public NodeSlotsChangedEventEmitter getNodeSlotsChangedEventEmitter() {
        return nodeSlotsChangedEventEmitter;
    }

    public BaseDirectoryChangedEventEmitter getBaseDirectoryChangedEventEmitter() {
        return baseDirectoryChangedEventEmitter;
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
            nodeSlotsChangedEventEmitter.emit(new NodeSlotsChangedEvent(this));
        }
    }

    private <T extends JIPipeDataSlot> boolean updateSlotOrder(List<String> configurationOrder, BiMap<String, T> slotMap, List<T> slots) {
        if (configurationOrder.size() == slots.size()) {
            boolean noChanges = true;
            for (int i = 0; i < configurationOrder.size(); i++) {
                if (!Objects.equals(configurationOrder.get(i), slots.get(i).getName())) {
                    noChanges = false;
                    break;
                }
            }
            if (noChanges) {
                return false;
            }
        }

        slots.clear();
        for (String name : configurationOrder) {
            JIPipeDataSlot instance = slotMap.getOrDefault(name, null);
            if (instance != null) {
                slots.add((T) instance);
            }
        }
        for (JIPipeDataSlot instance : slotMap.values()) {
            if (!slots.contains(instance)) {
                slots.add((T) instance);
            }
        }
        return true;
    }

    private <T extends JIPipeDataSlot> boolean updateRemoveSlots(boolean changed, Map<String, JIPipeDataSlotInfo> definedSlots, BiMap<String, T> slotMap, List<T> slots) {
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

    private <T extends JIPipeDataSlot> boolean updateAddAndModifySlots(boolean changed, Map.Entry<String, JIPipeDataSlotInfo> entry, BiMap<String, T> slotMap, List<T> slots) {
        JIPipeDataSlot existing = slotMap.getOrDefault(entry.getKey(), null);
        if (existing != null) {
            if (!Objects.equals(existing.getInfo(), entry.getValue())) {
                slotMap.remove(entry.getKey());
                slots.remove(existing);
                existing = null;
            }
        }
        if (existing == null) {
            existing = entry.getValue().createInstance(this);
            slotMap.put(entry.getKey(), (T) existing);
            slots.add((T) existing);
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
        return other.slotConfiguration.duplicate();
    }

    /**
     * Runs the workload
     *
     * @param runContext   the context of the run process
     * @param progressInfo progress passed from the runner
     */
    public abstract void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo);

    /**
     * Returns the algorithm name
     *
     * @return algorithm name
     */
    @JIPipeParameter(value = "jipipe:node:name", uiOrder = -9999, pinned = true, functional = false)
    @SetJIPipeDocumentation(name = "Name", description = "Custom algorithm name.")
    public String getName() {
        if (customName == null || customName.isEmpty()) {
            return getInfo().getName();
        }
        return customName;
    }

    @SetJIPipeDocumentation(name = "Lock location/size", description = "If enabled, lock the location and size of this node. Does not affect automated alignment operations. " +
            "Will lock the size if the node supports a resize handle. Will prevent deletion of the node by the user. Slots can still be edited/connected and parameters can still be changed.")
    @JIPipeParameter(value = "jipipe:node:ui-locked", pinned = true, functional = false, hidden = true)
    public boolean isUiLocked() {
        return uiLocked;
    }

    @JIPipeParameter("jipipe:node:ui-locked")
    public void setUiLocked(boolean uiLocked) {
        this.uiLocked = uiLocked;
    }

    /**
     * The current custom (user-defined) name
     *
     * @return the custom name
     */
    public String getCustomName() {
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
        if (ParameterUtils.isHiddenLocalParameter(tree, access, "jipipe:node:description", "jipipe:node:name", "jipipe:node:bookmarked")) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @SetJIPipeDocumentation(name = "Bookmark this node", description = "If enabled, the node is highlighted in the graph editor UI and added into the bookmark list.")
    @JIPipeParameter(value = "jipipe:node:bookmarked", pinned = true, functional = false, hidden = true)
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
    public Map<String, JIPipeInputDataSlot> getInputSlotMap() {
        return Collections.unmodifiableMap(inputSlotMap);
    }

    /**
     * Gets all output slot instances
     *
     * @return Current algorithm slots
     */
    public Map<String, JIPipeOutputDataSlot> getOutputSlotMap() {
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
    public List<JIPipeInputDataSlot> getInputSlots() {
        return Collections.unmodifiableList(inputSlots);
    }

    /**
     * Method that can be overwritten by child classes.
     * Should return slots that are actually used as data input.
     *
     * @return Input slots
     */
    public List<JIPipeInputDataSlot> getDataInputSlots() {
        return getInputSlots();
    }

    /**
     * Returns all output slots ordered by the slot order
     *
     * @return List of slots
     */
    public List<JIPipeOutputDataSlot> getOutputSlots() {
        return Collections.unmodifiableList(outputSlots);
    }

    /**
     * Triggered when a parameter is changed within the slot configuration.
     * Triggers a {@link NodeSlotsChangedEvent}
     *
     * @param event generated event
     */
    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if (event.getSource() == slotConfiguration) {
            getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
        }
    }

    /**
     * Should be triggered when a slot is added to the slot configuration
     *
     * @param event The event
     */
    @Override
    public void onSlotConfigurationChanged(JIPipeSlotConfiguration.SlotConfigurationChangedEvent event) {
        if (event.getConfiguration() == getSlotConfiguration()) {
            updateGraphNodeSlots();
        }
    }


    /**
     * Returns true if this node and the other node are functionally equal (i.e. they have the same functional parameters).
     * For non-functional nodes, this determines if the {@link JIPipeNodeInfo} is equal
     *
     * @param other the other node
     * @return if the nodes are functionally equal
     */
    @Override
    public boolean functionallyEquals(Object other) {
        if (other instanceof JIPipeGraphNode) {
            return getInfo() == ((JIPipeGraphNode) other).getInfo();
        }
        return false;
    }

    /**
     * Returns the UI class that is responsible for rendering this node
     *
     * @return the node UI class
     */
    public Class<? extends JIPipeDesktopGraphNodeUI> getNodeUiClass() {
        return JIPipeDesktopGraphNodeUI.class;
    }

    public JIPipePathMetadataStore getNodeMetadata() {
        return nodeMetadata;
    }

    public void setNodeMetadata(JIPipePathMetadataStore nodeMetadata) {
        this.nodeMetadata = nodeMetadata;
    }

    /**
     * Returns the location within the specified compartment or null if none is set
     *
     * @param compartment The compartment ID. Set to empty string for no compartment.
     * @return The UI location or null if unset
     */
    public Point getNodeUILocationWithin(String compartment) {
        compartment = StringUtils.orElse(compartment, "_");
        Integer x = nodeMetadata.getInteger(JIPipePathMetadataStore.key("location", compartment, "x"), null);
        Integer y = nodeMetadata.getInteger(JIPipePathMetadataStore.key("location", compartment, "y"), null);
        if (x == null || y == null) {
            return null;
        } else {
            return new Point(x, y);
        }
    }

    /**
     * Sets the UI location of this algorithm within the specified compartment
     *
     * @param compartment The compartment ID. Set to empty string for no compartment.
     * @param location    The UI location. Can be null to reset the location
     */
    public void setNodeUILocationWithin(String compartment, Point location) {
        compartment = StringUtils.orElse(compartment, "_");
        nodeMetadata.putPrimitive(JIPipePathMetadataStore.key("location", compartment, "x"), location.x);
        nodeMetadata.putPrimitive(JIPipePathMetadataStore.key("location", compartment, "y"), location.y);
    }

    /**
     * Sets the UI location of this algorithm within the specified compartment
     *
     * @param compartment The compartment ID
     * @param location    The UI location. Can be null to reset the location
     */
    public void setNodeUILocationWithin(UUID compartment, Point location) {
        setNodeUILocationWithin(StringUtils.nullToEmpty(compartment), location);
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
        jsonGenerator.writeStringField("jipipe:graph-compartment", StringUtils.nullToEmpty(getCompartmentUUIDInParentGraph()));
        jsonGenerator.writeStringField("jipipe:alias-id", StringUtils.nullToEmpty(getAliasIdInParentGraph()));
        jsonGenerator.writeObjectField("jipipe:slot-configuration", slotConfiguration);
        jsonGenerator.writeObjectField("jipipe:metadata-v1", nodeMetadata);
        jsonGenerator.writeStringField("jipipe:node-info-id", getInfo().getId());

        ParameterUtils.serializeParametersToJson(this, jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    /**
     * Loads this algorithm from JSON
     * Please do not override this method if absolutely necessary. Use onDeserialized() to add methods after deserialization
     *
     * @param node          The JSON data to load from
     * @param context       the context
     * @param issues        issues during deserializing. these should be severe issues (missing parameters etc.). if you want to notify the user about potential issues that can be acted upon, use the notification inbox
     * @param notifications additional notifications for the user. these can be acted upon
     */
    public void fromJson(JsonNode node, JIPipeValidationReportContext context, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        if (node.has("jipipe:slot-configuration")) {
            slotConfiguration.fromJson(node.get("jipipe:slot-configuration"));
        }
        if (node.has("jipipe:metadata-v1")) {
            try {
                nodeMetadata = JsonUtils.getObjectMapper().readerFor(JIPipePathMetadataStore.class).readValue(node.get("jipipe:metadata-v1"));
            } catch (IOException e) {
                context.error().title("Unable to load metadata").details(e.toString()).report(issues);
            }
        }

        // Migrate legacy grid location
        if (node.has("jipipe:ui-grid-location")) {
            for (Map.Entry<String, JsonNode> visualModeEntry : ImmutableList.copyOf(node.get("jipipe:ui-grid-location").fields())) {
                String compartment = visualModeEntry.getKey();
                for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(visualModeEntry.getValue().fields())) {
                    String visualMode = entry.getKey();
                    JsonNode xValue = entry.getValue().path("x");
                    JsonNode yValue = entry.getValue().path("y");
                    if ("VerticalCompact".equals(visualMode) && !xValue.isMissingNode() && !yValue.isMissingNode()) {
                        setNodeUILocationWithin(compartment, new Point(xValue.asInt(), yValue.asInt()));
                    }
                }
            }
        }

        // Deserialize algorithm-specific parameters
        ParameterUtils.deserializeParametersFromJson(this, node, context, issues);

        // Re-apply if they somehow got lost
        updateEnvironmentOverrides();

        // Run postprocess command
        onDeserialized(node, issues, notifications);
    }

    /**
     * Override this method to add operations to be run after deserialization from JSON
     *
     * @param node          the JSON node where the data was loaded
     * @param issues        issues during deserialization. if you want to notify the user about potential issues that can be acted upon, use the notification inbox
     * @param notifications additional notifications for the user. these can be acted upon
     */
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {

    }

    /**
     * The storage path is used in {@link JIPipeGraphRun} to indicate where output data is written
     * This is only used internally
     *
     * @return Storage path
     */
    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * Sets the storage path. Used by {@link JIPipeGraphRun}
     *
     * @param storagePath Storage path
     */
    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Returns the internal storage path relative to the output folder.
     * Used internally by {@link JIPipeGraphRun}
     *
     * @return Storage path relative to the output folder
     */
    public Path getInternalStoragePath() {
        return internalStoragePath;
    }

    /**
     * Sets the internal storage path relative to the output folder.
     * Used internally by {@link JIPipeGraphRun}
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
     * A use case is to convert algorithms from internal representations to their public variants (e.g. {@link JIPipeProjectCompartmentOutput}
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
    public JIPipeOutputDataSlot getOutputSlot(String name) {
        JIPipeOutputDataSlot slot = outputSlotMap.get(name);
        if (slot == null) {
            return null;
        }
        if (!slot.isOutput()) {
            throw new IllegalArgumentException("The slot " + name + " is not an output slot!");
        }
        return slot;
    }

    /**
     * Returns the input slot with the specified name.
     * Throws {@link NullPointerException} if the slot does not exist and {@link IllegalArgumentException} if the slot is not an input
     *
     * @param name Slot name
     * @return Slot instance
     */
    public JIPipeInputDataSlot getInputSlot(String name) {
        JIPipeInputDataSlot slot = inputSlotMap.get(name);
        if (slot == null) {
            return null;
        }
        if (!slot.isInput()) {
            throw new IllegalArgumentException("The slot " + name + " is not an input slot!");
        }
        return slot;
    }

    /**
     * Returns all input slots that do not have data set.
     *
     * @return List of slots
     */
    public List<JIPipeInputDataSlot> getOpenInputSlots() {
        List<JIPipeInputDataSlot> result = new ArrayList<>();
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            if (parentGraph.getInputIncomingSourceSlots(inputSlot).isEmpty()) {
                result.add(inputSlot);
            }
        }
        return result;
    }

    /**
     * Gets all input slots that have the specified role
     *
     * @param role the role
     * @return the list of input slots with the role. order is according to getInputSlots()
     */
    public List<JIPipeInputDataSlot> getInputSlotsWithRole(JIPipeDataSlotRole role) {
        return inputSlots.stream().filter(slot -> slot.getInfo().getRole() == role).collect(Collectors.toList());
    }

    /**
     * Gets all input slots that have the specified role
     *
     * @param role the role
     * @return the list of input slots with the role. order is according to getOutputSlots()
     */
    public List<JIPipeOutputDataSlot> getOutputSlotsWithRole(JIPipeDataSlotRole role) {
        return outputSlots.stream().filter(slot -> slot.getInfo().getRole() == role).collect(Collectors.toList());
    }

    /**
     * Returns the first output slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no output slot.
     *
     * @return Slot instance
     */
    public JIPipeOutputDataSlot getFirstOutputSlot() {
        return getOutputSlots().get(0);
    }

    /**
     * Returns the first input slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no input slot.
     *
     * @return Slot instance
     */
    public JIPipeInputDataSlot getFirstInputSlot() {
        return getInputSlots().get(0);
    }

    /**
     * Returns the last output slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no output slot.
     *
     * @return Slot instance
     */
    public JIPipeOutputDataSlot getLastOutputSlot() {
        List<JIPipeOutputDataSlot> outputSlots = getOutputSlots();
        return outputSlots.get(outputSlots.size() - 1);
    }

    /**
     * Returns the last input slot according to the slot order.
     * Throws {@link IndexOutOfBoundsException} if there is no input slot.
     *
     * @return Slot instance
     */
    public JIPipeInputDataSlot getLastInputSlot() {
        List<JIPipeInputDataSlot> inputSlots = getInputSlots();
        return inputSlots.get(inputSlots.size() - 1);
    }

    /**
     * Returns the graph this algorithm is located in.
     * Can be null.
     *
     * @return Graph instance or null
     */
    public JIPipeGraph getParentGraph() {
        return parentGraph;
    }

    /**
     * Sets the graph this algorithm is location in.
     * This has no side effects and is only for reference usage.
     *
     * @param parentGraph Graph instance or null
     */
    public void setParentGraph(JIPipeGraph parentGraph) {
        this.parentGraph = parentGraph;
    }

    /**
     * Returns the UUID within the current graph. Requires that getGraph() is not null.
     *
     * @return The UUID within getGraph()
     */
    public UUID getUUIDInParentGraph() {
        if (parentGraph != null) {
            return parentGraph.getUUIDOf(this);
        } else {
            return null;
        }
    }

    /**
     * Returns the compartment UUID within the current graph. Requires that getGraph() is not null.
     *
     * @return The UUID within getGraph()
     */
    public UUID getCompartmentUUIDInParentGraph() {
        if (parentGraph != null) {
            return parentGraph.getCompartmentUUIDOf(this);
        } else {
            return null;
        }
    }

    /**
     * Removes all location information added via setLocationWithin()
     */
    public void clearAllNodeUILocations() {
        nodeMetadata.clearEntriesWithPathPrefix(JIPipePathMetadataStore.key("location"));
    }

    /**
     * Returns the custom description that is set by the user
     *
     * @return Description or null
     */
    @SetJIPipeDocumentation(name = "Description", description = "A custom description")
    @StringParameterSettings(multiline = true)
    @JIPipeParameter(value = "jipipe:node:description", uiOrder = -999, pinned = true, functional = false)
    public HTMLText getCustomDescription() {
        if (customDescription == null) {
            customDescription = new HTMLText();
        }
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
     * Returns the current project associated with the current run
     *
     * @return the project
     */
    public JIPipeProject getRuntimeProject() {
        return runtimeProject;
    }

    /**
     * Sets the current project associated with the current run
     *
     * @param runtimeProject the project
     */
    public void setRuntimeProject(JIPipeProject runtimeProject) {
        this.runtimeProject = runtimeProject;
    }

    /**
     * Returns the current project directory.
     *
     * @return the project directory. Null if none was set.
     */
    public Path getProjectDirectory() {
        return projectDirectory;
    }

    /**
     * Sets the project directory. Can be null.
     *
     * @param projectDirectory the project directory. can be null.
     */
    public void setProjectDirectory(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    /**
     * Returns the current work directory of this algorithm. This is used internally to allow relative data paths.
     *
     * @return The current work directory or null.
     */
    public Path getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Sets the current work directory of this algorithm. This is used internally to allow loading data from relative paths.
     * This triggers a {@link BaseDirectoryChangedEvent} that can be received by {@link JIPipeDataSlot} instances to adapt to the work directory.
     *
     * @param baseDirectory The work directory. Can be null
     */
    public void setBaseDirectory(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        getBaseDirectoryChangedEventEmitter().emit(new BaseDirectoryChangedEvent(this, baseDirectory));
    }

    public void archiveReportValidation(JIPipeValidationReportContext context, JIPipeValidationReport report, Path originalBaseDirectory) {

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
            return JIPipe.getTemporaryDirectory("__nd");
        }
        return PathUtils.createTempSubDirectory(getScratchBaseDirectory(), "__nd");
    }

    /**
     * Triggers an event that indicates that the slots have changed
     */
    public void emitNodeSlotsChangedEvent() {
        getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
    }

    /**
     * Returns a list of all dependencies this algorithm currently has.
     *
     * @return set of dependencies
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
     * Clears all data slots
     *
     * @param force        if true, close all data stores
     * @param progressInfo the progress info
     */
    public void clearSlotData(boolean force, JIPipeProgressInfo progressInfo) {
        for (JIPipeDataSlot slot : inputSlots) {
            slot.clearData(force, progressInfo);
        }
        for (JIPipeDataSlot slot : outputSlots) {
            slot.clearData(force, progressInfo);
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
        return parentGraph.canUserDelete(this);
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

    public boolean isVisibleIn(UUID compartmentUUIDInGraph) {
        UUID currentCompartmentUUID = getCompartmentUUIDInParentGraph();
        if (Objects.equals(compartmentUUIDInGraph, currentCompartmentUUID)) {
            return true;
        }
        if (parentGraph == null) {
            return false;
        }
        return parentGraph.getVisibleCompartmentUUIDsOf(this).contains(compartmentUUIDInGraph);
    }

    /**
     * Returns a display name for the compartment
     *
     * @return the display name for the compartment
     */
    public String getCompartmentDisplayName() {
        String compartment = "";
        if (parentGraph != null) {
            UUID compartmentUUID = getCompartmentUUIDInParentGraph();
            if (compartmentUUID != null) {
                JIPipeProject project = parentGraph.getProject();
                if (project != null) {
                    JIPipeProjectCompartment projectCompartment = project.getCompartments().getOrDefault(compartmentUUID, null);
                    if (projectCompartment != null) {
                        compartment = projectCompartment.getName();
                    }
                }
                if (compartment == null) {
                    compartment = compartmentUUID.toString();
                }
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
        if (!StringUtils.isNullOrEmpty(compartment)) {
            return compartment + "/" + getName();
        } else {
            return getName();
        }
    }

    /**
     * Returns the compartment UUID in the graph as string.
     * If the UUID is null, an empty string is returned.
     *
     * @return UUID as string or an empty string of the compartment is null;
     */
    public String getCompartmentUUIDInGraphAsString() {
        return StringUtils.nullToEmpty(getCompartmentUUIDInParentGraph());
    }

    /**
     * Gathers all known external environments
     *
     * @param target             the list where the external environments will be gathered
     * @param configurationCache the cache for the environment configurations
     */
    public void getEnvironmentDependencies(List<JIPipeEnvironmentConfigurator<?>> target, JIPipeEnvironmentConfigurationCache configurationCache) {
        for (Class<? extends JIPipeEnvironment> environmentType : getRegisteredEnvironmentTypes()) {
            JIPipeEnvironmentConfigurator<? extends JIPipeEnvironment> reference = getEnvironmentConfigurator(environmentType, configurationCache);
            if (reference != null) {
                target.add(reference);
            }
        }
    }

    /**
     * Returns the alias ID of this node.
     * It is unique within the same graph, but should not be used to identify it due to dependency on the node's name.
     * Use the UUID instead.
     *
     * @return the alias ID. Null if there is no graph.
     */
    public String getAliasIdInParentGraph() {
        if (parentGraph != null) {
            return parentGraph.getAliasIdOf(this);
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
        JIPipeProject project = parentGraph.getAttachment(JIPipeProject.class);
        return project.getCompartments().getOrDefault(getCompartmentUUIDInParentGraph(), null);
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
     * An icon that is displayed on the right-hand side of the input slot
     * Can be null
     *
     * @param slotName the slot name
     * @return the icon or null
     */
    public ImageIcon getUIInputSlotIcon(String slotName) {
        JIPipeInputDataSlot inputSlot = getInputSlot(slotName);
        if (inputSlot != null && inputSlot.getInfo().getRole() == JIPipeDataSlotRole.ParametersLooping) {
            return JIPipe.RESOURCES.getIcon16Inverted("actions/reload.png");
        }
        return null;
    }

    /**
     * Explanations added to the slot menu
     * Can be null
     *
     * @param slotName the slot name
     * @param target   the list of menu items
     */
    public void createUIInputSlotIconDescriptionMenuItems(String slotName, List<ViewOnlyMenuItem> target) {
    }

    /**
     * Returns the currently accessible project data directories
     *
     * @return the project data directory map. empty map if the node is not associated to a project
     */
    public Map<String, Path> getProjectDataDirs() {
        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getUserPathMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        return projectDataDirs;
    }

    /**
     * Size of the icon returned by getUIInputSlotIcon
     * Should be at most 16x16
     * Defaults to 12x12
     *
     * @param slotName the slot name
     * @return the icon size
     */
    public Dimension getUIInputSlotIconBaseDimensions(String slotName) {
        return new Dimension(12, 12);
    }

    /**
     * Gets a description of the node as text
     *
     * @param stringBuilder the string builder
     */
    public void getTextDescription(StringBuilder stringBuilder) {
        JIPipeGraphNode referenceInstance = getInfo().newInstance();
        JIPipeParameterTree currentTree = new JIPipeParameterTree(this);
        JIPipeParameterTree referenceTree = new JIPipeParameterTree(referenceInstance);
        stringBuilder.append("<ul>");
        for (String key : currentTree.getParameters().keySet()) {
            if ("jipipe:node:name".equals(key)) {
                continue;
            }
            if ("jipipe:node:description".equals(key)) {
                continue;
            }
            JIPipeParameterAccess currentAccess = currentTree.getParameterAccess(key);
            JIPipeParameterAccess referenceAccess = referenceTree.getParameterAccess(key);
            Object reference = referenceAccess != null ? referenceAccess.get(Object.class) : null;
            Object obj = currentAccess.get(Object.class);
            if (referenceAccess == null || !Objects.equals(obj, reference)) {
                if (obj instanceof JIPipeListParameter) {
                    JIPipeListParameter<?> objects = (JIPipeListParameter<?>) obj;
                    for (int i = 0; i < objects.size(); i++) {
                        Object item = objects.get(i);
                        stringBuilder.append("<li>").append("The parameter item #").append(i + 1).append(" of \"").append(currentAccess.getName()).append("\"");
                        if (currentAccess.getSource() != this) {
                            stringBuilder.append(" in category \"").append(currentTree.getSourceDocumentationName(currentAccess.getSource())).append("\"");
                        }
                        String textDescription = JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(item);
                        stringBuilder.append(" (").append(key).append(") is set to <strong>")
                                .append(HtmlEscapers.htmlEscaper().escape(textDescription)).append("</strong></li>");
                    }
                } else {
                    stringBuilder.append("<li>").append("The parameter \"").append(currentAccess.getName()).append("\"");
                    if (currentAccess.getSource() != this) {
                        stringBuilder.append(" in category \"").append(currentTree.getSourceDocumentationName(currentAccess.getSource())).append("\"");
                    }
                    String textDescription = JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(obj);
                    stringBuilder.append(" (").append(key).append(") is set to <strong>")
                            .append(HtmlEscapers.htmlEscaper().escape(textDescription)).append("</strong></li>");

                }
            }
        }
        stringBuilder.append("</ul>");
    }

    public JIPipeProject getProject() {
        if (getRuntimeProject() == null && getParentGraph() != null) {
            return getParentGraph().getProject();
        } else {
            return getRuntimeProject();
        }
    }

    /**
     * Returns all node UI locations from the metadata map
     *
     * @return all UI locations
     */
    public Map<String, Point> getAllNodeUILocations() {
        Map<String, Point> result = new HashMap<>();
        for (Map.Entry<JIPipePathMetadataStore.Key, Object> entry : nodeMetadata.getEntriesUnderPath("location").entrySet()) {
            if (entry.getKey().getNameCount() == 3) {
                String compartmentName = entry.getKey().getName(1);
                String locationName = entry.getKey().getName(2);
                String standardCompartmentName = "_".equals(compartmentName) ? "" : compartmentName;
                if (locationName.equals("x")) {
                    result.put(standardCompartmentName, new Point(nodeMetadata.getInteger(JIPipePathMetadataStore.key("location", compartmentName, "x"), 0),
                            nodeMetadata.getInteger(JIPipePathMetadataStore.key("location", compartmentName, "y"), 0)));
                }
            }
        }
        return result;
    }

    /**
     * Executed by various functions that handle the addition of nodes into a pipeline
     * Allows for UI feedback (dialogs etc.)
     *
     * @param canvasUI the canvas where the node will be added. At this point the node is not yet inside the graph or canvas and does not have a UUID!
     * @return whether the node should be added into the canvas
     */
    public boolean uiBeforeAddToCanvas(JIPipeDesktopGraphCanvasUI canvasUI) {
        return true;
    }

    /**
     * Executed by various functions that handle the addition of nodes into a pipeline
     * Allows for UI feedback (dialogs etc.)
     *
     * @param canvasUI the canvas where the node was added.
     */
    public void uiAfterAddToCanvas(JIPipeDesktopGraphCanvasUI canvasUI) {
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        reportEnvironmentValidity(reportContext, reportSettings, report, progressInfo);
    }

    /**
     * Checks if all registered environments are available
     *
     * @param reportContext  the report context
     * @param reportSettings report settings
     * @param report         the report
     * @param progressInfo   the progress info
     */
    public void reportEnvironmentValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        JIPipeEnvironmentConfigurationCache configurationCache = new JIPipeEnvironmentConfigurationCache();
        for (Class<? extends JIPipeEnvironment> environmentType : getRegisteredEnvironmentTypes()) {
            JIPipeEnvironmentConfigurator<? extends JIPipeEnvironment> environmentConfigurator = getEnvironmentConfigurator(environmentType, configurationCache);
            if (!environmentConfigurator.generateValidityReport(reportContext, reportSettings, progressInfo).isValid()) {
                JIPipeEnvironmentsServiceComponent.EnvironmentInfo environmentInfo = JIPipe.getInstance().getEnvironments().getInfoByClass(environmentType);
                reportContext.error().title(environmentInfo.getName() + " environment not configured").explanation("The environment '" + environmentInfo.getId() + "' is not properly configured for the current node.")
                        .solution("Check if the node's environment overrides the " + environmentInfo.getName() + " environment and is correctly configured. Otherwise, check the project and application settings for the respective environment configuration.").report(report);
            }
        }
    }

    /**
     * Returns the set of utilized environment types within this node
     *
     * @return the unmodifiable set
     */
    public Set<Class<? extends JIPipeEnvironment>> getRegisteredEnvironmentTypes() {
        return Collections.unmodifiableSet(getInfo().getEnvironments());
    }

    /**
     * Returns the environment reference for the environment class
     *
     * @param klass the environment class
     * @param <T>   the environment type
     * @return the environment reference
     */
    public <T extends JIPipeEnvironment> JIPipeEnvironmentConfigurator<T> getEnvironmentConfigurator(Class<T> klass, JIPipeEnvironmentConfigurationCache configurationCache) {
        if (!getRegisteredEnvironmentTypes().contains(klass)) {
            throw new IllegalArgumentException("The node " + getDisplayName() + " (" + getInfo().getId() + ") tried to utilize an environment of type " + klass + " without prior registration. Please inform the developer of this node about this issue.");
        }
        return new JIPipeEnvironmentConfigurator<>(klass, configurationCache, this, getProject());
    }

    /**
     * Gets a fully configured environment
     *
     * @param klass              the environment class
     * @param configurationCache the environment cache
     * @param progressInfo       the progress info
     * @param <T>                the environment class
     * @return the environment
     */
    public <T extends JIPipeEnvironment> T getEnvironment(Class<T> klass, JIPipeEnvironmentConfigurationCache configurationCache, JIPipeProgressInfo progressInfo) {
        return getEnvironmentConfigurator(klass, configurationCache).get(progressInfo);
    }

    /**
     * Gets a fully configured environment
     *
     * @param klass        the environment class
     * @param runContext   the run context (contains an environment cache)
     * @param progressInfo the progress info
     * @param <T>          the environment class
     * @return the environment
     */
    public <T extends JIPipeEnvironment> T getEnvironment(Class<T> klass, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        return getEnvironmentConfigurator(klass, runContext.getEnvironmentConfigurationCache()).get(progressInfo);
    }

    /**
     * Convenience method to acquire a server instance lease.
     * The method:
     * 1. Looks up the factory for the given instance class
     * 2. Gets the associated environment class
     * 3. Resolves the environment via {@link JIPipeEnvironmentConfigurator}
     * 4. Acquires a lease from {@link org.hkijena.jipipe.api.service.components.JIPipeServerServiceComponent}
     * 5. Returns the lease
     *
     * <p>Usage:</p>
     * <pre>
     * try (JIPipeServerLease<MyServerInstance> lease = getServerInstance(MyServerInstance.class)) {
     *     lease.getInstance().doSomething();
     * }
     * </pre>
     *
     * @param instanceClass the server instance class
     * @param <TInst>       the server instance type
     * @return a lease for the server instance
     * @throws IllegalArgumentException if no factory is registered for the instance class
     */
    public <TInst extends JIPipeServerInstance<?>> JIPipeServerLease<TInst> getServerInstance(Class<TInst> instanceClass) {
        org.hkijena.jipipe.api.service.components.JIPipeServerServiceComponent serverService =
                org.hkijena.jipipe.JIPipe.getInstance().getServerService();

        // Find the factory that produces this instance class
        for (String factoryId : serverService.getFactoryIds()) {
            org.hkijena.jipipe.api.service.components.JIPipeServerServiceComponent.FactoryEntry<?, ?> entry =
                    serverService.getFactory(factoryId);
            if (entry.getInstClass().equals(instanceClass)) {
                // Resolve the environment
                Class<? extends JIPipeEnvironment> envClass = entry.getEnvClass();
                JIPipeEnvironment environment = getEnvironment(envClass, new JIPipeEnvironmentConfigurationCache(), new JIPipeProgressInfo());
                if (environment == null) {
                    throw new IllegalStateException("Unable to resolve environment of type " + envClass.getName() +
                            " for server instance " + instanceClass.getName());
                }
                @SuppressWarnings("unchecked")
                JIPipeServerLease<TInst> lease = serverService.acquireLease(factoryId, instanceClass, environment);
                return lease;
            }
        }
        throw new IllegalArgumentException("No server instance factory registered for class: " + instanceClass.getName());
    }

    @SetJIPipeDocumentation(name = "Override connected services", description = "Allows to configure service connectors for this specific node")
    @JIPipeParameter(value = "jipipe:environment-overrides", persistence = JIPipeParameterSerializationMode.Object, icon = "actions/environment.png")
    public JIPipeGraphNodeEnvironmentOverridesParameter getEnvironmentOverrides() {
        return environmentOverrides;
    }

    /**
     * First step of the archiving process that discovers all external paths.
     * This is required to find the correct path root for the second archive step.
     *
     * @param progressInfo the progress info
     * @return the external file paths
     */
    public Set<Path> archiveDiscoverExternalPaths(JIPipeProgressInfo progressInfo) {
        return Collections.emptySet();
    }

    /**
     * Second step of the archiving process. Executed after archiveDiscoverExternalPaths().
     *
     * @param updateMap    the map that updates the paths from the current to the archived (relative) paths. Contains only paths that have been returned by archiveDiscoverExternalPaths()
     * @param progressInfo the progress info
     */
    public void archiveUpdateExternalPaths(Map<Path, Path> updateMap, JIPipeProgressInfo progressInfo) {

    }

    public interface NodeSlotsChangedEventListener {
        void onNodeSlotsChanged(NodeSlotsChangedEvent event);
    }

    public interface BaseDirectoryChangedEventListener {
        void onBaseDirectoryChanged(BaseDirectoryChangedEvent event);
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

    /**
     * Triggered when the base directory of a project or algorithm was changed
     */
    public static class BaseDirectoryChangedEvent extends AbstractJIPipeEvent {
        private final Path baseDirectory;

        /**
         * @param baseDirectory the work directory
         */
        public BaseDirectoryChangedEvent(Object source, Path baseDirectory) {
            super(source);
            this.baseDirectory = baseDirectory;
        }

        public Path getBaseDirectory() {
            return baseDirectory;
        }
    }

    public static class BaseDirectoryChangedEventEmitter extends JIPipeEventEmitter<BaseDirectoryChangedEvent, BaseDirectoryChangedEventListener> {
        @Override
        protected void call(BaseDirectoryChangedEventListener baseDirectoryChangedEventListener, BaseDirectoryChangedEvent event) {
            baseDirectoryChangedEventListener.onBaseDirectoryChanged(event);
        }
    }

    /**
     * Triggered when an algorithm's slots change
     */
    public static class NodeSlotsChangedEvent extends AbstractJIPipeEvent {
        private final JIPipeGraphNode node;

        /**
         * @param node the algorithm
         */
        public NodeSlotsChangedEvent(JIPipeGraphNode node) {
            super(node);
            this.node = node;
        }

        public JIPipeGraphNode getNode() {
            return node;
        }
    }

    public static class NodeSlotsChangedEventEmitter extends JIPipeEventEmitter<NodeSlotsChangedEvent, NodeSlotsChangedEventListener> {

        @Override
        protected void call(NodeSlotsChangedEventListener nodeSlotsChangedEventListener, NodeSlotsChangedEvent event) {
            nodeSlotsChangedEventListener.onNodeSlotsChanged(event);
        }
    }
}
