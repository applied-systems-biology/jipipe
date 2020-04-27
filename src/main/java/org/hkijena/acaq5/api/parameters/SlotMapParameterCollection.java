package org.hkijena.acaq5.api.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Parameter that holds a value for each data slot
 */
public class SlotMapParameterCollection extends ACAQDynamicParameterCollection {
    private ACAQAlgorithm algorithm;
    private Class<?> dataClass;

    /**
     * Creates a new instance
     *
     * @param dataClass the data type of the parameter assigned to each slot
     * @param algorithm the algorithm that contains the slots
     */
    public SlotMapParameterCollection(Class<?> dataClass, ACAQAlgorithm algorithm) {
        this.dataClass = dataClass;
        this.algorithm = algorithm;
        this.setAllowUserModification(false);
        updateSlots();
        this.algorithm.getEventBus().register(this);
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    public Class<?> getDataClass() {
        return dataClass;
    }

    /**
     * Method that adds missing entries, or removes invalid entries based on the algorithm's slot configuration
     */
    public void updateSlots() {
        if (algorithm != null) {
            Set<String> toRemove = new HashSet<>();
            for (String slotName : getCustomParameters().keySet()) {
                if (!algorithm.getSlots().containsKey(slotName)) {
                    toRemove.add(slotName);
                }
            }
            for (String slotName : toRemove) {
                removeParameter(slotName);
            }
            for (String slotName : algorithm.getSlots().keySet()) {
                if (!containsKey(slotName)) {
                    addParameter(slotName, dataClass);
                }
            }
        }
    }

    /**
     * Triggered when algorithm slots are changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onSlotsUpdated(AlgorithmSlotsChangedEvent event) {
        updateSlots();
    }
}
