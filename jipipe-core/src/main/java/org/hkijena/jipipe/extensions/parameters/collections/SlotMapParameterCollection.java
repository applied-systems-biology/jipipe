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

package org.hkijena.jipipe.extensions.parameters.collections;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.events.NodeSlotsChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;

import java.util.function.Supplier;

/**
 * Parameter that holds a value for each data slot
 */
public abstract class SlotMapParameterCollection extends JIPipeDynamicParameterCollection {
    private JIPipeGraphNode algorithm;
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
    public SlotMapParameterCollection(Class<?> dataClass, JIPipeGraphNode algorithm, Supplier<Object> newInstanceGenerator, boolean initialize) {
        this.dataClass = dataClass;
        this.algorithm = algorithm;
        this.newInstanceGenerator = newInstanceGenerator;
        this.setAllowUserModification(false);
        if (initialize)
            updateSlots();
        this.algorithm.getEventBus().register(this);
    }

    public JIPipeGraphNode getAlgorithm() {
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
    public void onSlotsUpdated(NodeSlotsChangedEvent event) {
        updateSlots();
    }

    public Supplier<Object> getNewInstanceGenerator() {
        return newInstanceGenerator;
    }

    public void setNewInstanceGenerator(Supplier<Object> newInstanceGenerator) {
        this.newInstanceGenerator = newInstanceGenerator;
    }
}
