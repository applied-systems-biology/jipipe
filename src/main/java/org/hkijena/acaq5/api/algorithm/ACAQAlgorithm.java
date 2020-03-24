package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.data.*;
import org.hkijena.acaq5.api.data.traits.*;
import org.hkijena.acaq5.api.events.*;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors.StringParameterSettings;
import org.hkijena.acaq5.utils.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * An algorithm is is a set of input and output data slots, and a run() function
 * It is part of the {@link ACAQAlgorithmGraph}
 */
@JsonSerialize(using = ACAQAlgorithm.Serializer.class)
public abstract class ACAQAlgorithm implements ACAQValidatable, ACAQParameterHolder {
    private ACAQAlgorithmDeclaration declaration;
    private ACAQSlotConfiguration slotConfiguration;
    private ACAQTraitConfiguration traitConfiguration;
    private Map<String, ACAQDataSlot> slots = new HashMap<>();
    private EventBus eventBus = new EventBus();
    private Map<String, Point> locations = new HashMap<>();
    private Path internalStoragePath;
    private Path storagePath;
    private String customName;
    private String customDescription;
    private String compartment;
    private Set<String> visibleCompartments = new HashSet<>();
    private ACAQAlgorithmGraph graph;
    private Path workDirectory;

    /**
     * Initializes this algorithm with a custom provided slot configuration and trait configuration
     *
     * @param declaration        Contains algorithm metadata
     * @param slotConfiguration  if null, generate the slot configuration
     * @param traitConfiguration if null, defaults to {@link ACAQDefaultMutableTraitConfiguration}
     */
    public ACAQAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        this.declaration = declaration;
        if (slotConfiguration == null) {
            ACAQMutableSlotConfiguration.Builder builder = ACAQMutableSlotConfiguration.builder();

            for (AlgorithmInputSlot slot : getClass().getAnnotationsByType(AlgorithmInputSlot.class)) {
                if (slot.autoCreate()) {
                    builder.addInputSlot(slot.slotName(), slot.value());
                }
            }
            for (AlgorithmOutputSlot slot : getClass().getAnnotationsByType(AlgorithmOutputSlot.class)) {
                if (slot.autoCreate()) {
                    if (!slot.inheritedSlot().isEmpty())
                        builder.allowOutputSlotInheritance(true);
                    builder.addOutputSlot(slot.slotName(), slot.inheritedSlot(), slot.value());
                }
            }

            builder.seal();
            slotConfiguration = builder.build();
        }
        if (traitConfiguration == null) {
            traitConfiguration = new ACAQDefaultMutableTraitConfiguration(this);
        }

        this.slotConfiguration = slotConfiguration;
        this.traitConfiguration = traitConfiguration;
        slotConfiguration.getEventBus().register(this);
        traitConfiguration.getEventBus().register(this);
        initalize();
        initializeTraits();
    }

    public ACAQAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        this(declaration, slotConfiguration, null);
    }

    public ACAQAlgorithm(ACAQAlgorithmDeclaration declaration) {
        this(declaration, null, null);
    }

    /**
     * Copies the input algorithm's properties into this algorithm
     *
     * @param other
     */
    public ACAQAlgorithm(ACAQAlgorithm other) {
        this.declaration = other.declaration;
        this.slotConfiguration = copySlotConfiguration(other);
        this.traitConfiguration = new ACAQDefaultMutableTraitConfiguration(this);
        this.locations = new HashMap<>(other.locations);
        this.compartment = other.compartment;
        this.visibleCompartments = new HashSet<>(other.visibleCompartments);
        this.customName = other.customName;
        slotConfiguration.getEventBus().register(this);
        traitConfiguration.getEventBus().register(this);
        initalize();
        initializeTraits();
    }

    /**
     * Initializes the trait configuration.
     * This includes applying annotation-based trait assignments
     */
    protected void initializeTraits() {
        if (getTraitConfiguration() instanceof ACAQDefaultMutableTraitConfiguration) {
            ACAQDefaultMutableTraitConfiguration traitConfiguration = (ACAQDefaultMutableTraitConfiguration) getTraitConfiguration();
            // Annotation-based trait configuration
            if (getClass().getAnnotationsByType(ConfigTraits.class).length > 0) {
                ConfigTraits configTraits = getClass().getAnnotationsByType(ConfigTraits.class)[0];
                traitConfiguration.setTransferAllToAll(configTraits.autoTransfer());
                traitConfiguration.getMutableGlobalTraitModificationTasks().merge(getDeclaration().getSlotTraitConfiguration());
                traitConfiguration.setTraitModificationsSealed(!configTraits.allowModify());
            } else {
                traitConfiguration.setTransferAllToAll(true);
                traitConfiguration.getMutableGlobalTraitModificationTasks().merge(getDeclaration().getSlotTraitConfiguration());
                traitConfiguration.setTraitModificationsSealed(true);
            }
            traitConfiguration.setTraitTransfersSealed(true);
        }
    }

    /**
     * Copies the slot configuration from the other algorithm to this algorithm
     * Override this method for special configuration cases
     *
     * @param other
     * @return
     */
    protected ACAQSlotConfiguration copySlotConfiguration(ACAQAlgorithm other) {
        ACAQMutableSlotConfiguration configuration = ACAQMutableSlotConfiguration.builder().build();
        configuration.setTo(other.slotConfiguration);
        return configuration;
    }

    private void initalize() {
        for (Map.Entry<String, ACAQSlotDefinition> kv : slotConfiguration.getSlots().entrySet()) {
            slots.put(kv.getKey(), new ACAQDataSlot(this, kv.getValue().getSlotType(), kv.getKey(), kv.getValue().getDataClass()));
        }
    }

    public abstract void run();

    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQParameter(value = "name", visibility = ACAQParameterVisibility.Visible)
    @ACAQDocumentation(name = "Name", description = "Custom algorithm name.")
    public String getName() {
        if (customName == null || customName.isEmpty())
            return getDeclaration().getName();
        return customName;
    }

    @ACAQParameter("name")
    public void setCustomName(String customName) {
        this.customName = customName;
        getEventBus().post(new ParameterChangedEvent(this, "name"));
    }

    public ACAQAlgorithmCategory getCategory() {
        return getDeclaration().getCategory();
    }

    Set<ACAQTraitDeclaration> getPreferredTraits() {
        return getDeclaration().getPreferredTraits();
    }

    Set<ACAQTraitDeclaration> getUnwantedTraits() {
        return getDeclaration().getUnwantedTraits();
    }

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public Map<String, ACAQDataSlot> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public List<String> getInputSlotOrder() {
        return getSlotConfiguration().getInputSlotOrder();
    }

    public List<String> getOutputSlotOrder() {
        return getSlotConfiguration().getOutputSlotOrder();
    }

    public List<ACAQDataSlot> getInputSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (String key : getInputSlotOrder()) {
            if (slots.containsKey(key))
                result.add(slots.get(key));
        }
        return Collections.unmodifiableList(result);
    }

    public List<ACAQDataSlot> getOutputSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (String key : getOutputSlotOrder()) {
            if (slots.containsKey(key))
                result.add(slots.get(key));
        }
        return Collections.unmodifiableList(result);
    }

    @Subscribe
    public void onSlotAdded(SlotAddedEvent event) {
        ACAQSlotDefinition definition = slotConfiguration.getSlots().get(event.getSlotName());
        slots.put(definition.getName(), new ACAQDataSlot(this, definition.getSlotType(), definition.getName(), definition.getDataClass()));
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
        updateSlotInheritance();
    }

    @Subscribe
    public void onSlotRemoved(SlotRemovedEvent event) {
        slots.remove(event.getSlotName());
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
        updateSlotInheritance();
    }

    @Subscribe
    public void onSlotRenamed(SlotRenamedEvent event) {
        ACAQDataSlot slot = slots.get(event.getOldSlotName());
        slots.remove(event.getOldSlotName());
        slots.put(event.getNewSlotName(), slot);
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
        updateSlotInheritance();
    }

    @Subscribe
    public void onSlotOrderChanged(SlotOrderChangedEvent event) {
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
        updateSlotInheritance();
    }

    public Map<String, Point> getLocations() {
        return locations;
    }

    public Point getLocationWithin(String compartment) {
        return locations.getOrDefault(compartment, null);
    }

    public void setLocationWithin(String compartment, Point location) {
        this.locations.put(compartment, location);
    }

    public void fromJson(JsonNode node) {

        // Load compartment
        compartment = node.get("acaq:algorithm-compartment").asText();

        if (node.has("acaq:slot-configuration"))
            slotConfiguration.fromJson(node.get("acaq:slot-configuration"));
        if (node.has("acaq:algorithm-ui-location")) {
            for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(node.get("acaq:algorithm-ui-location").fields())) {
                JsonNode xValue = entry.getValue().path("x");
                JsonNode yValue = entry.getValue().path("y");
                if (!xValue.isMissingNode() && !yValue.isMissingNode()) {
                    locations.put(entry.getKey(), new Point(xValue.asInt(), yValue.asInt()));
                }
            }
        }
        if (node.has("acaq:trait-generation") && getTraitConfiguration() instanceof ACAQMutableTraitConfiguration) {
            ((ACAQDefaultMutableTraitConfiguration) getTraitConfiguration()).fromJson(node.get("acaq:trait-generation"));
        }

        // Deserialize dynamic parameters
        Map<String, ACAQDynamicParameterHolder> dynamicParameterHolders = ACAQDynamicParameterHolder.findDynamicParameterHolders(this);
        for (Map.Entry<String, ACAQDynamicParameterHolder> entry : dynamicParameterHolders.entrySet()) {
            JsonNode entryNode = node.path("acaq:dynamic-parameters").path(entry.getKey());
            if (!entryNode.isMissingNode()) {
                entry.getValue().fromJson(entryNode);
            }
        }

        // Deserialize algorithm-specific parameters
        for (Map.Entry<String, ACAQParameterAccess> kv : ACAQParameterAccess.getParameters(this).entrySet()) {
            if (node.has(kv.getKey())) {
                Object v;
                try {
                    v = JsonUtils.getObjectMapper().readerFor(kv.getValue().getFieldClass()).readValue(node.get(kv.getKey()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                kv.getValue().set(v);
            }
        }
    }

    /**
     * The storage path is used in {@link ACAQRun} to indicate where output data is written
     * This is only used internally
     *
     * @return
     */
    public Path getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    public ACAQTraitConfiguration getTraitConfiguration() {
        return traitConfiguration;
    }

    public Path getInternalStoragePath() {
        return internalStoragePath;
    }

    public void setInternalStoragePath(Path internalStoragePath) {
        this.internalStoragePath = internalStoragePath;
    }

    public ACAQAlgorithmDeclaration getDeclaration() {
        return declaration;
    }

    public String getCompartment() {
        return compartment;
    }

    public void setCompartment(String compartment) {
        this.compartment = compartment;
    }

    public boolean isVisibleIn(String containerCompartment) {
        return containerCompartment.equals(compartment) || visibleCompartments.contains(containerCompartment);
    }

    public Set<String> getVisibleCompartments() {
        return visibleCompartments;
    }

    public void setVisibleCompartments(Set<String> visibleCompartments) {
        this.visibleCompartments = visibleCompartments;
    }

    public ACAQDataSlot getOutputSlot(String name) {
        ACAQDataSlot slot = slots.get(name);
        if (!slot.isOutput())
            throw new IllegalArgumentException("The slot " + name + " is not an output slot!");
        return slot;
    }

    public ACAQDataSlot getInputSlot(String name) {
        ACAQDataSlot slot = slots.get(name);
        if (!slot.isInput())
            throw new IllegalArgumentException("The slot " + name + " is not an input slot!");
        return slot;
    }

    public List<ACAQDataSlot> getOpenInputSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            if (graph.getSourceSlot(inputSlot) == null) {
                result.add(inputSlot);
            }
        }
        return result;
    }

    public ACAQDataSlot getFirstOutputSlot() {
        return getOutputSlots().get(0);
    }

    public ACAQDataSlot getFirstInputSlot() {
        return getInputSlots().get(0);
    }

    public ACAQDataSlot getLastOutputSlot() {
        List<ACAQDataSlot> outputSlots = getOutputSlots();
        return outputSlots.get(outputSlots.size() - 1);
    }

    public ACAQDataSlot getLastInputSlot() {
        List<ACAQDataSlot> inputSlots = getInputSlots();
        return inputSlots.get(inputSlots.size() - 1);
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public void setGraph(ACAQAlgorithmGraph graph) {
        this.graph = graph;
    }

    public String getIdInGraph() {
        return graph.getIdOf(this);
    }

    /**
     * Removes all location information added via setLocationWithin()
     */
    public void clearLocations() {
        locations.clear();
    }

    @ACAQDocumentation(name = "Description", description = "A custom description")
    @StringParameterSettings(multiline = true)
    @ACAQParameter(value = "description", visibility = ACAQParameterVisibility.Visible)
    public String getCustomDescription() {
        return customDescription;
    }

    @ACAQParameter("description")
    public void setCustomDescription(String customDescription) {
        this.customDescription = customDescription;
    }

    public Path getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(Path workDirectory) {
        this.workDirectory = workDirectory;
        eventBus.post(new WorkDirectoryChangedEvent(workDirectory));
    }

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

        ACAQDataSlot inheritedSlot = getSlots().getOrDefault(inheritedSlotName, null);
        if (inheritedSlot == null || inheritedSlot.getSlotType() != ACAQDataSlot.SlotType.Input)
            return slotInstance.getAcceptedDataType();

        // Inherit from the inherited slot if there is no graph connection
        ACAQDataSlot sourceSlot = graph.getSourceSlot(inheritedSlot);
        if (sourceSlot == null)
            return ACAQSlotDefinition.applyInheritanceConversion(slotDefinition, inheritedSlot.getAcceptedDataType());
        return ACAQSlotDefinition.applyInheritanceConversion(slotDefinition, sourceSlot.getAcceptedDataType());
    }

    public void updateSlotInheritance() {
        if (graph != null) {
            boolean modified = false;
            for (Map.Entry<String, ACAQSlotDefinition> entry : getSlotConfiguration().getOutputSlots().entrySet()) {
                ACAQDataSlot slotInstance = getSlots().getOrDefault(entry.getKey(), null);
                if (slotInstance == null || slotInstance.getSlotType() != ACAQDataSlot.SlotType.Output)
                    continue;

                Class<? extends ACAQData> expectedSlotDataType = getExpectedSlotDataType(entry.getValue(), slotInstance);
                if (slotInstance.getAcceptedDataType() != expectedSlotDataType) {
                    slotInstance.setAcceptedDataType(expectedSlotDataType);
                    eventBus.post(new AlgorithmSlotsChangedEvent(this));
                    modified = true;
                }
            }
            if (modified) {
                Set<ACAQAlgorithm> algorithms = new HashSet<>();
                for (ACAQDataSlot slot : getOutputSlots()) {
                    for (ACAQDataSlot targetSlot : graph.getTargetSlots(slot)) {
                        algorithms.add(targetSlot.getAlgorithm());
                    }
                }
                for (ACAQAlgorithm algorithm : algorithms) {
                    algorithm.updateSlotInheritance();
                }
            }
        }
    }

    public void onSlotDisconnected(AlgorithmGraphDisconnectedEvent event) {
        updateSlotInheritance();
    }

    /**
     * Returns a list of all dependencies
     *
     * @return
     */
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = new HashSet<>();
        result.add(ACAQAlgorithmRegistry.getInstance().getSourceOf(getDeclaration().getId()));

        // Add traits
        for (ACAQTraitTransferTask task : getTraitConfiguration().getTransferTasks()) {
            for (ACAQTraitDeclaration traitRestriction : task.getTraitRestrictions()) {
                result.add(ACAQTraitRegistry.getInstance().getSourceOf(traitRestriction.getId()));
            }
        }
        for (ACAQDataSlotTraitConfiguration configuration : getTraitConfiguration().getModificationTasks().values()) {
            for (ACAQTraitDeclaration traitDeclaration : configuration.getOperations().keySet()) {
                result.add(ACAQTraitRegistry.getInstance().getSourceOf(traitDeclaration.getId()));
            }
        }

        // Add data slots
        for (Map.Entry<String, ACAQDataSlot> entry : slots.entrySet()) {
            result.add(ACAQDatatypeRegistry.getInstance().getSourceOf(entry.getValue().getAcceptedDataType()));
            for (ACAQTraitDeclaration traitDeclaration : entry.getValue().getSlotAnnotations()) {
                result.add(ACAQTraitRegistry.getInstance().getSourceOf(traitDeclaration.getId()));
            }
        }

        return result;
    }

    /**
     * Utility function to create an algorithm instance from its id
     *
     * @param id
     * @param <T>
     * @return
     */
    public static <T extends ACAQAlgorithm> T newInstance(String id) {
        return (T) ACAQAlgorithmRegistry.getInstance().getDeclarationById(id).newInstance();
    }

    public static class Serializer extends JsonSerializer<ACAQAlgorithm> {
        @Override
        public void serialize(ACAQAlgorithm algorithm, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("acaq:slot-configuration", algorithm.slotConfiguration);
            jsonGenerator.writeFieldName("acaq:algorithm-ui-location");
            jsonGenerator.writeStartObject();
            for (Map.Entry<String, Point> entry : algorithm.locations.entrySet()) {
                if (entry.getValue() != null) {
                    jsonGenerator.writeFieldName(entry.getKey());
                    jsonGenerator.writeStartObject();
                    jsonGenerator.writeNumberField("x", entry.getValue().x);
                    jsonGenerator.writeNumberField("y", entry.getValue().y);
                    jsonGenerator.writeEndObject();
                }
            }
            jsonGenerator.writeEndObject();
            jsonGenerator.writeStringField("acaq:algorithm-type", algorithm.getDeclaration().getId());
            jsonGenerator.writeStringField("acaq:algorithm-compartment", algorithm.getCompartment());
            for (Map.Entry<String, ACAQParameterAccess> kv : ACAQParameterAccess.getParameters(algorithm).entrySet()) {
                jsonGenerator.writeObjectField(kv.getKey(), kv.getValue().get());
            }
            if (algorithm.getTraitConfiguration() instanceof ACAQMutableTraitConfiguration) {
                jsonGenerator.writeObjectField("acaq:trait-generation", algorithm.getTraitConfiguration());
            }

            // Save dynamic parameter storages
            jsonGenerator.writeFieldName("acaq:dynamic-parameters");
            jsonGenerator.writeStartObject();
            Map<String, ACAQDynamicParameterHolder> dynamicParameterHolders = ACAQDynamicParameterHolder.findDynamicParameterHolders(algorithm);
            for (Map.Entry<String, ACAQDynamicParameterHolder> entry : dynamicParameterHolders.entrySet()) {
                jsonGenerator.writeObjectField(entry.getKey(), entry.getValue());
            }
            jsonGenerator.writeEndObject();

            jsonGenerator.writeEndObject();
        }
    }
}
