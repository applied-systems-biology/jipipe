package org.hkijena.acaq5.api.data.traits;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.TraitConfigurationChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.util.*;

/**
 * The default {@link ACAQTraitConfiguration}. It transfers traits from input slots, to output slots.
 * During this transfer, traits can be added or removed.
 */
@JsonSerialize(using = ACAQDefaultMutableTraitConfiguration.Serializer.class)
public class ACAQDefaultMutableTraitConfiguration implements ACAQMutableTraitConfiguration {

    private EventBus eventBus = new EventBus();
    private ACAQAlgorithm algorithm;
    private Map<String, ACAQDataSlotTraitConfiguration> slotTraitModificationTasks = new HashMap<>();
    private ACAQDataSlotTraitConfiguration globalTraitModificationTasks = new ACAQDataSlotTraitConfiguration();
    private List<ACAQTraitTransferTask> transferTasks = new ArrayList<>();
    private boolean transferAllToAll = false;

    private boolean traitModificationsSealed = false;
    private boolean traitTransfersSealed = false;

    /**
     * Creates a new instance
     *
     * @param algorithm The algorithm
     */
    public ACAQDefaultMutableTraitConfiguration(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public Map<String, ACAQDataSlotTraitConfiguration> getModificationTasks() {
        Map<String, ACAQDataSlotTraitConfiguration> result = new HashMap<>();
        for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
            ACAQDataSlotTraitConfiguration configuration = new ACAQDataSlotTraitConfiguration();
            configuration.merge(globalTraitModificationTasks);
            configuration.merge(slotTraitModificationTasks.getOrDefault(outputSlot.getName(), new ACAQDataSlotTraitConfiguration()));
            result.put(outputSlot.getName(), configuration);
        }
        return result;
    }

    @Override
    public List<ACAQTraitTransferTask> getTransferTasks() {
        if (transferAllToAll) {
            List<ACAQTraitTransferTask> result = new ArrayList<>();
            for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
                for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                    result.add(new ACAQTraitTransferTask(inputSlot.getName(), outputSlot.getName(), Collections.emptySet()));
                }
            }

            return result;
        } else {
            return Collections.unmodifiableList(transferTasks);
        }
    }

    @Override
    public void apply() {
        for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
            outputSlot.clearSlotAnnotations();
        }
        applyTransfer();
        applyModification();
    }

    private void applyModification() {
        for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
            globalTraitModificationTasks.applyTo(outputSlot);
            slotTraitModificationTasks.getOrDefault(outputSlot.getName(), new ACAQDataSlotTraitConfiguration()).applyTo(outputSlot);
        }
    }

    private void applyTransfer() {
//        System.out.println("APPLY_TRANSFER");
        if (transferAllToAll) {
            for (ACAQDataSlot sourceSlot : algorithm.getInputSlots()) {
                for (ACAQDataSlot targetSlot : algorithm.getOutputSlots()) {
                    for (ACAQTraitDeclaration slotAnnotation : sourceSlot.getSlotAnnotations()) {
                        targetSlot.addSlotAnnotation(slotAnnotation);
//                        System.out.println("TRANSFER " + slotAnnotation.getName() + " FROM " + sourceSlot.getNameWithAlgorithmName() + " TO " + targetSlot.getNameWithAlgorithmName());
                    }
                }
            }
        } else {
            for (ACAQTraitTransferTask transferTask : transferTasks) {
                transferTask.applyTo(algorithm);
            }
        }
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return True if traits are automatically transferred from all input slots to all output slots
     */
    public boolean isTransferAllToAll() {
        return transferAllToAll;
    }

    /**
     * Enable to transfer traits automatically from all input slots to all output slots
     *
     * @param transferAllToAll Enabled
     */
    public void setTransferAllToAll(boolean transferAllToAll) {
        this.transferAllToAll = transferAllToAll;
        postChangedEvent();
    }

    /**
     * @return Mutable list of transfer tasks
     */
    public List<ACAQTraitTransferTask> getMutableTransferTasks() {
        return transferTasks;
    }

    /**
     * @param transferTasks List of transfer tasks
     */
    public void setMutableTransferTasks(List<ACAQTraitTransferTask> transferTasks) {
        this.transferTasks = transferTasks;
        postChangedEvent();
    }

    /**
     * Loads from JSON
     *
     * @param jsonNode JSON data
     */
    public void fromJson(JsonNode jsonNode) {
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();

        JsonNode modificationNode = jsonNode.path("modification");
        if (!modificationNode.isMissingNode()) {
            try {
                TypeReference<HashMap<String, ACAQDataSlotTraitConfiguration>> typeRef = new TypeReference<HashMap<String, ACAQDataSlotTraitConfiguration>>() {
                };
                this.slotTraitModificationTasks = objectMapper.readerFor(typeRef).readValue(modificationNode.get("per-slot"));
            } catch (IOException e) {
                throw new UserFriendlyRuntimeException(e, "Unable to read annotation configuration from JSON!", "Algorithm " + algorithm.getName(),
                        "There is essential information missing in the JSON data.",
                        "Please check if the JSON data is valid.");
            }
            try {
                TypeReference<ACAQDataSlotTraitConfiguration> typeRef = new TypeReference<ACAQDataSlotTraitConfiguration>() {
                };
                this.globalTraitModificationTasks = objectMapper.readerFor(typeRef).readValue(modificationNode.get("global"));
            } catch (IOException e) {
                throw new UserFriendlyRuntimeException(e, "Unable to read annotation configuration from JSON!", "Algorithm " + algorithm.getName(),
                        "There is essential information missing in the JSON data.",
                        "Please check if the JSON data is valid.");
            }
        }

        JsonNode transferNode = jsonNode.path("transfer");
        if (!transferNode.isMissingNode()) {
            this.transferAllToAll = transferNode.get("transfer-all-to-all").asBoolean();
            try {
                TypeReference<ArrayList<ACAQTraitTransferTask>> typeRef = new TypeReference<ArrayList<ACAQTraitTransferTask>>() {
                };
                this.transferTasks = objectMapper.readerFor(typeRef).readValue(transferNode.get("transfers"));
            } catch (IOException e) {
                throw new UserFriendlyRuntimeException(e, "Unable to read annotation configuration from JSON!", "Algorithm " + algorithm.getName(),
                        "There is essential information missing in the JSON data.",
                        "Please check if the JSON data is valid.");
            }
        }
    }

    /**
     * Returns a mutable slot configuration
     *
     * @param slotName Slot name
     * @return Slot configuration
     */
    public ACAQDataSlotTraitConfiguration getConfigurationForSlot(String slotName) {
        ACAQDataSlotTraitConfiguration result = slotTraitModificationTasks.getOrDefault(slotName, null);
        if (result == null) {
            result = new ACAQDataSlotTraitConfiguration();
            slotTraitModificationTasks.put(slotName, result);
        }
        return result;
    }

    @Override
    public boolean isTraitModificationsSealed() {
        return traitModificationsSealed;
    }

    /**
     * Seals/unseals trait modifications
     *
     * @param traitModificationsSealed seal/unseal modfications
     */
    public void setTraitModificationsSealed(boolean traitModificationsSealed) {
        this.traitModificationsSealed = traitModificationsSealed;
        postChangedEvent();
    }

    @Override
    public void addTransfer(ACAQTraitTransferTask task) {
        transferTasks.add(task);
        postChangedEvent();
    }

    @Override
    public void removeTransfer(ACAQTraitTransferTask task) {
        transferTasks.removeIf(t -> t.equals(task));
        postChangedEvent();
    }

    @Override
    public boolean isTraitTransfersSealed() {
        return traitTransfersSealed;
    }

    /**
     * Seals/unseals trait transfers
     *
     * @param traitTransfersSealed If sealed or unsealed
     */
    public void setTraitTransfersSealed(boolean traitTransfersSealed) {
        this.traitTransfersSealed = traitTransfersSealed;
        postChangedEvent();
    }

    @Override
    public void setTraitModification(String slotName, ACAQTraitDeclaration traitDeclaration, ACAQTraitModificationOperation operation) {
        getConfigurationForSlot(slotName).set(traitDeclaration, operation);
        postChangedEvent();
    }

    /**
     * Post an event that the configuration was changed
     */
    public void postChangedEvent() {
        eventBus.post(new TraitConfigurationChangedEvent(this));
    }

    /**
     * @return Modification tasks
     */
    public Map<String, ACAQDataSlotTraitConfiguration> getMutableSlotTraitModificationTasks() {
        return slotTraitModificationTasks;
    }

    /**
     * Sets the modification tasks
     *
     * @param slotTraitModificationTasks List of modification tasks
     */
    public void setMutableSlotTraitModificationTasks(Map<String, ACAQDataSlotTraitConfiguration> slotTraitModificationTasks) {
        this.slotTraitModificationTasks = slotTraitModificationTasks;
        postChangedEvent();
    }

    /**
     * @return Modification tasks
     */
    public ACAQDataSlotTraitConfiguration getMutableGlobalTraitModificationTasks() {
        return globalTraitModificationTasks;
    }

    /**
     * Sets modification tasks
     *
     * @param globalTraitModificationTasks Modification tasks
     */
    public void setMutableGlobalTraitModificationTasks(ACAQDataSlotTraitConfiguration globalTraitModificationTasks) {
        this.globalTraitModificationTasks = globalTraitModificationTasks;
        postChangedEvent();
    }

    /**
     * Removes all configuration
     */
    public void clear() {
        slotTraitModificationTasks.clear();
        transferTasks.clear();
        globalTraitModificationTasks.clear();
        postChangedEvent();
    }

    /**
     * Serializes the configuration
     */
    public static class Serializer extends JsonSerializer<ACAQDefaultMutableTraitConfiguration> {
        @Override
        public void serialize(ACAQDefaultMutableTraitConfiguration traitConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeFieldName("modification");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("per-slot", traitConfiguration.slotTraitModificationTasks);
            jsonGenerator.writeObjectField("global", traitConfiguration.globalTraitModificationTasks);
            jsonGenerator.writeEndObject();
            jsonGenerator.writeFieldName("transfer");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("transfers", traitConfiguration.transferTasks);
            jsonGenerator.writeBooleanField("transfer-all-to-all", traitConfiguration.transferAllToAll);
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }
    }
}
