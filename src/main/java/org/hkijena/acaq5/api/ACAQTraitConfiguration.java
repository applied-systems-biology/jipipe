package org.hkijena.acaq5.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Processes algorithm traits of an {@link ACAQAlgorithm}
 */
public class ACAQTraitConfiguration {

    private ACAQSlotConfiguration slotConfiguration;
    private List<ModifyTask> modifyTasks = new ArrayList<>();
    private List<TransferTask> transferTasks = new ArrayList<>();

    public ACAQTraitConfiguration(ACAQSlotConfiguration slotConfiguration) {
        this.slotConfiguration = slotConfiguration;
    }


    /**
     * Removes a trait from the specified output slot
     * @param outputSlotName
     * @param trait
     * @return
     */
    public ACAQTraitConfiguration removesTraitFrom(String outputSlotName, Class<? extends ACAQTrait> trait) {
        if(slotConfiguration.getSlots().get(outputSlotName).getSlotType() != ACAQDataSlot.SlotType.Output)
            throw new IllegalArgumentException("Slot must be an output slot!");
        modifyTasks.add(new ModifyTask(outputSlotName, ModificationType.REMOVE, trait));
        return this;
    }

    /**
     * Adds a trait to the specified output slot
     * @param outputSlotName
     * @param trait
     * @return
     */
    public ACAQTraitConfiguration addsTraitTo(String outputSlotName, Class<? extends ACAQTrait> trait) {
        if(slotConfiguration.getSlots().get(outputSlotName).getSlotType() != ACAQDataSlot.SlotType.Output)
            throw new IllegalArgumentException("Slot must be an output slot!");
        modifyTasks.add(new ModifyTask(outputSlotName, ModificationType.ADD, trait));
        return this;
    }

    /**
     * Removes a trait from all output slots
     * @param trait
     * @return
     */
    public ACAQTraitConfiguration removesTrait(Class<? extends ACAQTrait> trait) {
        modifyTasks.add(new ModifyTask(null, ModificationType.REMOVE, trait));
        return this;
    }

    /**
     * Adds a trait to all output slots
     * @param trait
     * @return
     */
    public ACAQTraitConfiguration addsTrait(Class<? extends ACAQTrait> trait) {
        modifyTasks.add(new ModifyTask(null, ModificationType.ADD, trait));
        return this;
    }

    /**
     * Transfers traits from the input slot to the specified output slot
     * @param inputSlotName
     * @param outputSlotName
     * @return
     */
    public ACAQTraitConfiguration transferFromTo(String inputSlotName, String outputSlotName) {
        if(slotConfiguration.getSlots().get(outputSlotName).getSlotType() != ACAQDataSlot.SlotType.Output)
            throw new IllegalArgumentException("Slot must be an output slot!");
        if(slotConfiguration.getSlots().get(inputSlotName).getSlotType() != ACAQDataSlot.SlotType.Input)
            throw new IllegalArgumentException("Slot must be an inpout slot!");
        transferTasks.add(new TransferTask(inputSlotName, outputSlotName));
        return this;
    }

    /**
     * Transfers the union of input slot traits to the specified output slot
     * @param outputSlotName
     * @return this
     */
    public ACAQTraitConfiguration transferFromAllTo(String outputSlotName) {
        if(slotConfiguration.getSlots().get(outputSlotName).getSlotType() != ACAQDataSlot.SlotType.Output)
            throw new IllegalArgumentException("Slot must be an output slot!");
        transferTasks.add(new TransferTask(null, outputSlotName));
        return this;
    }

    /**
     * Transfers the union of input slot traits to all output slots
     */
    public ACAQTraitConfiguration transferFromAllToAll() {
        transferTasks.clear();
        transferTasks.add(new TransferTask(null, null));
        return this;
    }

    /**
     * Removes all modifications and transfers
     */
    public ACAQTraitConfiguration clear() {
        modifyTasks.clear();
        transferTasks.clear();
        return this;
    }

    /**
     * Transfers traits from an input slot to an output slot
     * @param sourceSlotName Input slot name
     * @param source Input slot traits
     * @param targetSlotName Output slot name
     * @param target Output slot traits
     */
    public void transfer(String sourceSlotName, Set<Class<? extends ACAQTrait>> source,
                         String targetSlotName, Set<Class<? extends ACAQTrait>> target) {
        for(TransferTask task : transferTasks) {
            if((task.inputSlotName == null || task.inputSlotName.equals(sourceSlotName)) &&
                    (task.outputSlotName == null || task.outputSlotName.equals(targetSlotName))) {
                target.addAll(source);
            }
        }
    }

    /**
     * Modifies the traits of a slot
     * This function is applied to output slots after transfer
     * @param slotName Output slot name
     * @param target Existing output slot traits
     */
    public void modify(String slotName, Set<Class<? extends ACAQTrait>> target) {
        for(ModifyTask task : modifyTasks) {
            if(task.slotName == null || slotName.equals(task.slotName)) {
                switch(task.type) {
                    case ADD:
                        target.add(task.trait);
                        break;
                    case REMOVE:
                        target.removeIf(klass -> task.trait.isAssignableFrom(klass));
                        break;
                }
            }
        }
    }

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    /**
     * Describes a trait modification task
     * This is run on output slots only
     */
    public static class ModifyTask {
        private String slotName;
        private ModificationType type;
        private Class<? extends ACAQTrait> trait;

        /**
         *
         * @param slotName The affected input slot name. If null, all output slots are affected.
         * @param type The modification type
         * @param trait The trait that should be added/removed
         */
        public ModifyTask(String slotName, ModificationType type, Class<? extends ACAQTrait> trait) {
            this.slotName = slotName;
            this.type = type;
            this.trait = trait;
        }

        public String getSlotName() {
            return slotName;
        }

        public ModificationType getType() {
            return type;
        }

        public Class<? extends ACAQTrait> getTrait() {
            return trait;
        }
    }

    /**
     * Describes a task that transfers traits between an input slot and an output slot
     */
    public static class TransferTask {
        private String inputSlotName;
        private String outputSlotName;

        /**
         *
         * @param inputSlotName The input slot name. If null, the set of source traits will be the union of all input slot traits.
         * @param outputSlotName The output slot name. If null, all output slots are affected.
         */
        public TransferTask(String inputSlotName, String outputSlotName) {
            this.inputSlotName = inputSlotName;
            this.outputSlotName = outputSlotName;
        }

        public String getInputSlotName() {
            return inputSlotName;
        }

        public String getOutputSlotName() {
            return outputSlotName;
        }
    }

    public enum ModificationType {
        ADD,
        REMOVE
    }
}
