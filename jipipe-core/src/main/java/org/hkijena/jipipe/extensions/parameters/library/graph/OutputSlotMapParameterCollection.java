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

package org.hkijena.jipipe.extensions.parameters.library.graph;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.extensions.parameters.api.graph.SlotMapParameterCollection;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * @param initialize           If true, update the slots on creation
     */
    public OutputSlotMapParameterCollection(Class<?> dataClass, JIPipeGraphNode algorithm, Supplier<Object> newInstanceGenerator, boolean initialize) {
        super(dataClass, algorithm, (info) -> newInstanceGenerator.get(), initialize);
    }

    @Override
    public void updateSlots() {
        if (getAlgorithm() != null) {
            Set<String> toRemove = new HashSet<>();
            for (String slotName : getParameters().keySet()) {
                if (!getAlgorithm().hasOutputSlot(slotName)) {
                    toRemove.add(slotName);
                }
            }
            for (String slotName : toRemove) {
                removeParameter(slotName);
            }
            for (int i = 0; i < getAlgorithm().getOutputSlots().size(); i++) {
                JIPipeDataSlot slot = getAlgorithm().getOutputSlots().get(i);
                if (!containsKey(slot.getName())) {
                    JIPipeMutableParameterAccess access = addParameter(slot.getName(), getDataClass());
                    access.setUIOrder(i);
                    if (getNewInstanceGenerator() != null)
                        access.set(getNewInstanceGenerator().apply(slot.getInfo()));
                    else
                        access.set(ReflectionUtils.newInstance(getDataClass()));
                } else {
                    JIPipeMutableParameterAccess access = (JIPipeMutableParameterAccess) get(slot.getName());
                    access.setUIOrder(i);
                }
            }
        }
    }
}
