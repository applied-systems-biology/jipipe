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
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.events.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
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
public abstract class ACAQAlgorithm implements ACAQValidatable {
    private ACAQAlgorithmDeclaration declaration;
    private ACAQSlotConfiguration slotConfiguration;
    private ACAQTraitConfiguration traitConfiguration;
    private Map<String, ACAQDataSlot> slots = new HashMap<>();
    private EventBus eventBus = new EventBus();
    private Map<String, Point> locations = new HashMap<>();
    private Path internalStoragePath;
    private Path storagePath;
    private String customName;
    private String compartment;
    private Set<String> visibleCompartments = new HashSet<>();
    private ACAQAlgorithmGraph graph;

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
                    builder.addOutputSlot(slot.slotName(), slot.value());
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
            if (getClass().getAnnotationsByType(AutoTransferTraits.class).length > 0) {
                traitConfiguration.setTransferAllToAll(true);
            }
            traitConfiguration.getMutableGlobalTraitModificationTasks().merge(getDeclaration().getSlotTraitConfiguration());
            traitConfiguration.setTraitModificationsSealed(true);
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

    @ACAQParameter("name")
    @ACAQDocumentation(name = "Name")
    public String getName() {
        if (customName == null || customName.isEmpty())
            return getDeclaration().getName();
        return customName;
    }

    @ACAQParameter("name")
    public void setCustomName(String customName) {
        this.customName = customName;
        eventBus.post(new AlgorithmNameChanged(this));
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
    }

    @Subscribe
    public void onSlotRemoved(SlotRemovedEvent event) {
        slots.remove(event.getSlotName());
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    @Subscribe
    public void onSlotRenamed(SlotRenamedEvent event) {
        ACAQDataSlot slot = slots.get(event.getOldSlotName());
        slots.remove(event.getOldSlotName());
        slots.put(event.getNewSlotName(), slot);
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    @Subscribe
    public void onSlotOrderChanged(SlotOrderChangedEvent event) {
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
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

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public void setGraph(ACAQAlgorithmGraph graph) {
        this.graph = graph;
    }

    public String getIdInGraph() {
        return graph.getIdOf(this);
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
            jsonGenerator.writeEndObject();
        }
    }
}
