package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.*;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.traits.*;
import org.hkijena.acaq5.utils.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

@JsonSerialize(using = ACAQAlgorithm.Serializer.class)
public abstract class ACAQAlgorithm {
    private ACAQSlotConfiguration slotConfiguration;
    private ACAQTraitConfiguration traitConfiguration;
    private Map<String, ACAQDataSlot<?>> slots = new HashMap<>();
    private EventBus eventBus = new EventBus();
    private Point location;
    private Path storagePath;

    public ACAQAlgorithm(ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
       this.slotConfiguration = slotConfiguration;
       this.traitConfiguration = traitConfiguration;
       slotConfiguration.getEventBus().register(this);
       initalize();
       initializeTraits();
    }

    public ACAQAlgorithm(ACAQSlotConfiguration slotConfiguration) {
        this(slotConfiguration, new ACAQMutableTraitConfiguration(slotConfiguration));
    }

    public ACAQAlgorithm(ACAQAlgorithm other) {
        this.slotConfiguration = copySlotConfiguration(other);
        this.location = other.location;
        slotConfiguration.getEventBus().register(this);
        initalize();
        initializeTraits();
    }

    /**
     * Copies the slot configuration from the other algorithm to this algorithm
     * Override this method for special configuration cases
     * @param other
     * @return
     */
    protected ACAQSlotConfiguration copySlotConfiguration(ACAQAlgorithm other) {
        ACAQMutableSlotConfiguration configuration = ACAQMutableSlotConfiguration.builder().build();
        configuration.setTo(other.slotConfiguration);
        return configuration;
    }

    public static ACAQAlgorithm clone(ACAQAlgorithm other) {
        try {
            return ConstructorUtils.getMatchingAccessibleConstructor(other.getClass(), other.getClass()).newInstance(other);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void initalize() {
        for(Map.Entry<String, ACAQSlotDefinition> kv : slotConfiguration.getSlots().entrySet()) {
            slots.put(kv.getKey(), ACAQDataSlot.createInstance(this, kv.getValue()));
        }
    }

    /**
     * Initializes the trait configuration.
     * This includes applying annotation-based trait assignments
     */
    protected void initializeTraits() {
        if (getTraitConfiguration() instanceof ACAQMutableTraitConfiguration) {
            ACAQMutableTraitConfiguration traitConfiguration = (ACAQMutableTraitConfiguration) getTraitConfiguration();
            // Annotation-based trait configuration
            if (getClass().getAnnotationsByType(AutoTransferTraits.class).length > 0) {
                traitConfiguration.transferFromAllToAll();
            }
            for (AddsTrait trait : getClass().getAnnotationsByType(AddsTrait.class)) {
                traitConfiguration.addsTrait(trait.value());
            }
            for (RemovesTrait trait : getClass().getAnnotationsByType(RemovesTrait.class)) {
                traitConfiguration.removesTrait(trait.value());
            }

            // TODO: Registry-based trait configuration
        }
    }

    public abstract void run();

    public EventBus getEventBus() {
        return eventBus;
    }

    public String getName() {
        return getNameOf(getClass());
    }

    public ACAQAlgorithmCategory getCategory() {
        return getCategoryOf(getClass());
    }

    Set<Class<? extends ACAQTrait>> getPreferredTraits() {
        return getPreferredTraitsOf(getClass());
    }

    Set<Class<? extends ACAQTrait>> getUnwantedTraits() {
        return getUnwantedTraitsOf(getClass());
    }

    /**
     * Returns the name of an algorithm
     * @param klass
     * @return
     */
    public static String getNameOf(Class<? extends  ACAQAlgorithm> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if(annotations.length > 0) {
            return annotations[0].name();
        }
        else {
            return klass.getSimpleName();
        }
    }

    /**
     * Returns the name of an algorithm
     * @param klass
     * @return
     */
    public static ACAQAlgorithmCategory getCategoryOf(Class<? extends  ACAQAlgorithm> klass) {
        ACAQAlgorithmMetadata[] annotations = klass.getAnnotationsByType(ACAQAlgorithmMetadata.class);
        if(annotations.length > 0) {
            return annotations[0].category();
        }
        else {
            return ACAQAlgorithmCategory.Internal;
        }
    }

    /**
     * Returns all traits marked as preferred by the algorithm
     * @param klass
     * @return
     */
    public static Set<Class<? extends ACAQTrait>> getPreferredTraitsOf(Class<? extends  ACAQAlgorithm> klass) {
        Set<Class<? extends ACAQTrait>> result = new HashSet<>();
        for(GoodForTrait trait : klass.getAnnotationsByType(GoodForTrait.class)) {
            result.add(trait.value());
        }
        return result;
    }

    /**
     * Returns all traits marked as unwanted by the algorithm
     * @param klass
     * @return
     */
    public static Set<Class<? extends ACAQTrait>> getUnwantedTraitsOf(Class<? extends  ACAQAlgorithm> klass) {
        Set<Class<? extends ACAQTrait>> result = new HashSet<>();
        for(BadForTrait trait : klass.getAnnotationsByType(BadForTrait.class)) {
            result.add(trait.value());
        }
        return result;
    }

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public Map<String, ACAQDataSlot<?>> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public List<String> getInputSlotOrder() {
        return getSlotConfiguration().getInputSlotOrder();
    }

    public List<String> getOutputSlotOrder() {
        return getSlotConfiguration().getOutputSlotOrder();
    }

    public List<ACAQDataSlot<?>> getInputSlots() {
        List<ACAQDataSlot<?>> result = new ArrayList<>();
        for(String key : getInputSlotOrder()) {
           result.add(slots.get(key));
        }
        return Collections.unmodifiableList(result);
    }

    public List<ACAQDataSlot<?>> getOutputSlots() {
        List<ACAQDataSlot<?>> result = new ArrayList<>();
        for(String key : getOutputSlotOrder()) {
            result.add(slots.get(key));
        }
        return Collections.unmodifiableList(result);
    }

    @Subscribe
    public void onSlotAdded(SlotAddedEvent event) {
        ACAQSlotDefinition definition = slotConfiguration.getSlots().get(event.getSlotName());
        slots.put(definition.getName(), ACAQDataSlot.createInstance(this, definition));
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    @Subscribe
    public void onSlotRemoved(SlotRemovedEvent event) {
        slots.remove(event.getSlotName());
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    @Subscribe
    public void onSlotRenamed(SlotRenamedEvent event) {
        ACAQDataSlot<?> slot = slots.get(event.getOldSlotName());
        slots.remove(event.getOldSlotName());
        slots.put(event.getNewSlotName(), slot);
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    @Subscribe
    public void onSlotOrderChanged(SlotOrderChangedEvent event) {
        eventBus.post(new AlgorithmSlotsChangedEvent(this));
    }

    /**
     * Gets the location within UI representations
     * @return
     */
    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public static <T extends ACAQAlgorithm> T createInstance(Class<? extends T> klass) {
        try {
            return klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void fromJson(JsonNode node) {
        if(node.has("acaq:slot-configuration"))
            slotConfiguration.fromJson(node.get("acaq:slot-configuration"));
        if(node.has("acaq:algorithm-location-x") && node.has("acaq:algorithm-location-y")) {
            location = new Point();
            location.x = node.get("acaq:algorithm-location-x").asInt();
            location.y = node.get("acaq:algorithm-location-y").asInt();
        }

        // Deserialize algorithm-specific parameters
        for(Map.Entry<String, ACAQParameterAccess> kv : ACAQParameterAccess.getParameters(this).entrySet()) {
            if(node.has(kv.getKey())) {
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

    public static class Serializer extends JsonSerializer<ACAQAlgorithm> {
        @Override
        public void serialize(ACAQAlgorithm algorithm, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("acaq:slot-configuration", algorithm.slotConfiguration);
            jsonGenerator.writeObjectField("acaq:algorithm-class", algorithm.getClass().getCanonicalName());
            jsonGenerator.writeNumberField("acaq:algorithm-location-x", algorithm.location.x);
            jsonGenerator.writeNumberField("acaq:algorithm-location-y", algorithm.location.y);
            for(Map.Entry<String, ACAQParameterAccess> kv : ACAQParameterAccess.getParameters(algorithm).entrySet()) {
                jsonGenerator.writeObjectField(kv.getKey(), kv.getValue().get());
            }
            jsonGenerator.writeEndObject();
        }
    }
}
