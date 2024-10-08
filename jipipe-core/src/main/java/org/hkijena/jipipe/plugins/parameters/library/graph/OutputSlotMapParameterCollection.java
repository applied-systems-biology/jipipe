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

package org.hkijena.jipipe.plugins.parameters.library.graph;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.plugins.parameters.api.graph.SlotMapParameterCollection;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Parameter that holds a value for each data slot
 */
public class OutputSlotMapParameterCollection extends SlotMapParameterCollection {
    /**
     * Creates a new instance
     *
     * @param dataClass            the data type of the parameter assigned to each slot
     * @param algorithm            the algorithm that contains the slots
     * @param newInstanceGenerator optional method that generated new instances. Can be null
     * @param initialize           If true, update the slots on creation
     */
    public OutputSlotMapParameterCollection(Class<?> dataClass, JIPipeGraphNode algorithm, Function<JIPipeDataSlotInfo, Object> newInstanceGenerator, boolean initialize) {
        super(dataClass, algorithm, newInstanceGenerator, initialize);
    }

    /**
     * Creates a new instance
     *
     * @param dataClass            the data type of the parameter assigned to each slot
     * @param algorithm            the algorithm that contains the slots
     * @param newInstanceGenerator optional method that generated new instances. Can be null
     */
    public OutputSlotMapParameterCollection(Class<?> dataClass, JIPipeGraphNode algorithm, Function<JIPipeDataSlotInfo, Object> newInstanceGenerator) {
        super(dataClass, algorithm, newInstanceGenerator, false);
    }

    /**
     * Creates a new instance
     *
     * @param dataClass the data type of the parameter assigned to each slot
     * @param algorithm the algorithm that contains the slots
     */
    public OutputSlotMapParameterCollection(Class<?> dataClass, JIPipeGraphNode algorithm) {
        super(dataClass, algorithm, null, false);
    }

    @Override
    public void updateSlots() {
        if (getNode() != null) {
            Set<String> toRemove = new HashSet<>();
            for (String slotName : getParameters().keySet()) {
                if (!getNode().hasOutputSlot(slotName)) {
                    toRemove.add(slotName);
                }
            }
            for (String slotName : toRemove) {
                removeParameter(slotName);
            }
            for (int i = 0; i < getNode().getOutputSlots().size(); i++) {
                JIPipeDataSlot slot = getNode().getOutputSlots().get(i);
                if (!containsKey(slot.getName())) {

                    if (getSlotFilter() != null && !getSlotFilter().test(slot))
                        continue;

                    Object newValue;
                    if (getNewInstanceGenerator() != null)
                        newValue = (getNewInstanceGenerator().apply(slot.getInfo()));
                    else
                        newValue = (ReflectionUtils.newInstance(getDataClass()));
                    JIPipeMutableParameterAccess access = addParameter(slot.getName(), getDataClass());
                    access.set(newValue);
                    access.setUIOrder(i);
                } else {
                    JIPipeMutableParameterAccess access = (JIPipeMutableParameterAccess) get(slot.getName());
                    access.setUIOrder(i);
                }
            }
        }
    }
}
