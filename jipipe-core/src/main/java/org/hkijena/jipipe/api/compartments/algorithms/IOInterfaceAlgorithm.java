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

package org.hkijena.jipipe.api.compartments.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that passes the input to the output
 */
@JIPipeDocumentation(name = "IO Interface", description = "Passes its input to its output without changes.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class IOInterfaceAlgorithm extends JIPipeAlgorithm {

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link JIPipeGraphNode}'s newInstance() method
     *
     * @param info The algorithm info
     */
    public IOInterfaceAlgorithm(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
    }

    /**
     * Creates a copy of the other algorithm
     *
     * @param other The original
     */
    public IOInterfaceAlgorithm(IOInterfaceAlgorithm other) {
        super(other);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeDataSlot outputSlot = getOutputSlot(inputSlot.getName());
            outputSlot.copyFrom(inputSlot);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {

    }

    /**
     * Removes the interface and directly connects the inputs and outputs
     *
     * @param algorithm the algorithm
     */
    public static void collapse(IOInterfaceAlgorithm algorithm) {
        JIPipeGraph graph = algorithm.getGraph();
        Map<String, JIPipeDataSlot> inputSourceMap = new HashMap<>();
        Map<String, Set<JIPipeDataSlot>> outputTargetMap = new HashMap<>();
        for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
            JIPipeDataSlot sourceSlot = graph.getSourceSlot(inputSlot);
            if (sourceSlot != null) {
                inputSourceMap.put(inputSlot.getName(), sourceSlot);
            }
        }
        for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
            outputTargetMap.put(outputSlot.getName(), graph.getTargetSlots(outputSlot));
        }

        graph.removeNode(algorithm, false);

        for (Map.Entry<String, JIPipeDataSlot> entry : inputSourceMap.entrySet()) {
            JIPipeDataSlot source = entry.getValue();
            for (JIPipeDataSlot target : outputTargetMap.getOrDefault(entry.getKey(), Collections.emptySet())) {
                graph.connect(source, target);
            }
        }

    }

    /**
     * Replaces a {@link JIPipeCompartmentOutput} by an equivalent {@link IOInterfaceAlgorithm}
     *
     * @param compartmentOutput the output to be replaced
     */
    public static void replaceCompartmentOutput(JIPipeCompartmentOutput compartmentOutput) {
        JIPipeGraph graph = compartmentOutput.getGraph();
        String id = compartmentOutput.getIdInGraph();
        IOInterfaceAlgorithm ioInterfaceAlgorithm = JIPipeAlgorithm.newInstance("io-interface");
        ioInterfaceAlgorithm.setCustomName(compartmentOutput.getName());
        ioInterfaceAlgorithm.setCustomDescription(compartmentOutput.getCustomDescription());
        ioInterfaceAlgorithm.getSlotConfiguration().setTo(compartmentOutput.getSlotConfiguration());

        Map<String, JIPipeDataSlot> inputSourceMap = new HashMap<>();
        Map<String, Set<JIPipeDataSlot>> outputTargetMap = new HashMap<>();
        for (JIPipeDataSlot inputSlot : compartmentOutput.getInputSlots()) {
            JIPipeDataSlot sourceSlot = graph.getSourceSlot(inputSlot);
            if (sourceSlot != null) {
                inputSourceMap.put(inputSlot.getName(), sourceSlot);
            }
        }
        for (JIPipeDataSlot outputSlot : compartmentOutput.getOutputSlots()) {
            outputTargetMap.put(outputSlot.getName(), graph.getTargetSlots(outputSlot));
        }
        graph.removeNode(compartmentOutput, false);
        graph.insertNode(id, ioInterfaceAlgorithm, compartmentOutput.getCompartment());
        for (Map.Entry<String, JIPipeDataSlot> entry : inputSourceMap.entrySet()) {
            JIPipeDataSlot target = ioInterfaceAlgorithm.getInputSlot(entry.getKey());
            graph.connect(entry.getValue(), target);
        }
        for (Map.Entry<String, Set<JIPipeDataSlot>> entry : outputTargetMap.entrySet()) {
            JIPipeDataSlot source = ioInterfaceAlgorithm.getOutputSlot(entry.getKey());
            for (JIPipeDataSlot target : entry.getValue()) {
                graph.connect(source, target);
            }
        }
    }
}
