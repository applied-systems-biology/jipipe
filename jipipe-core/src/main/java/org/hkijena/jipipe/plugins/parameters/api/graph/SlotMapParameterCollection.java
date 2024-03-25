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

package org.hkijena.jipipe.plugins.parameters.api.graph;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Parameter that holds a value for each data slot
 */
public abstract class SlotMapParameterCollection extends JIPipeDynamicParameterCollection implements JIPipeGraphNode.NodeSlotsChangedEventListener {
    private JIPipeGraphNode node;
    private Class<?> dataClass;
    private Function<JIPipeDataSlotInfo, Object> newInstanceGenerator;

    private Predicate<JIPipeDataSlot> slotFilter;

    /**
     * Creates a new instance
     *
     * @param dataClass            the data type of the parameter assigned to each slot
     * @param node                 the algorithm that contains the slots
     * @param newInstanceGenerator optional method that generated new instances. Can be null
     * @param initialize           If true, update the slots on creation
     */
    public SlotMapParameterCollection(Class<?> dataClass, JIPipeGraphNode node, Function<JIPipeDataSlotInfo, Object> newInstanceGenerator, boolean initialize) {
        this.dataClass = dataClass;
        this.node = node;
        this.newInstanceGenerator = newInstanceGenerator;
        this.setAllowUserModification(false);
        if (initialize)
            updateSlots();
        this.node.getNodeSlotsChangedEventEmitter().subscribe(this);
    }

    public Predicate<JIPipeDataSlot> getSlotFilter() {
        return slotFilter;
    }

    public void setSlotFilter(Predicate<JIPipeDataSlot> slotFilter) {
        this.slotFilter = slotFilter;
    }

    public JIPipeGraphNode getNode() {
        return node;
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
    @Override
    public void onNodeSlotsChanged(JIPipeGraphNode.NodeSlotsChangedEvent event) {
        updateSlots();
    }

    public Function<JIPipeDataSlotInfo, Object> getNewInstanceGenerator() {
        return newInstanceGenerator;
    }

    public void setNewInstanceGenerator(Function<JIPipeDataSlotInfo, Object> newInstanceGenerator) {
        this.newInstanceGenerator = newInstanceGenerator;
    }
}
