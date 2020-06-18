package org.hkijena.acaq5.extensions.parameters.collections;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;

import java.util.function.Supplier;

/**
 * Parameter that holds a value for each data slot
 */
public abstract class SlotMapParameterCollection extends ACAQDynamicParameterCollection {
    private ACAQGraphNode algorithm;
    private Class<?> dataClass;
    private Supplier<Object> newInstanceGenerator;

    /**
     * Creates a new instance
     *
     * @param dataClass            the data type of the parameter assigned to each slot
     * @param algorithm            the algorithm that contains the slots
     * @param newInstanceGenerator optional method that generated new instances. Can be null
     * @param initialize           If true, update the slots on creation
     */
    public SlotMapParameterCollection(Class<?> dataClass, ACAQGraphNode algorithm, Supplier<Object> newInstanceGenerator, boolean initialize) {
        this.dataClass = dataClass;
        this.algorithm = algorithm;
        this.newInstanceGenerator = newInstanceGenerator;
        this.setAllowUserModification(false);
        if (initialize)
            updateSlots();
        this.algorithm.getEventBus().register(this);
    }

    public ACAQGraphNode getAlgorithm() {
        return algorithm;
    }

    public Class<?> getDataClass() {
        return dataClass;
    }

    /**
     * Method that adds missing entries, or removes invalid entries based on the algorithm's slot configuration
     */
    public abstract void updateSlots();

    /**
     * Triggered when algorithm slots are changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onSlotsUpdated(AlgorithmSlotsChangedEvent event) {
        updateSlots();
    }

    public Supplier<Object> getNewInstanceGenerator() {
        return newInstanceGenerator;
    }

    public void setNewInstanceGenerator(Supplier<Object> newInstanceGenerator) {
        this.newInstanceGenerator = newInstanceGenerator;
    }
}
