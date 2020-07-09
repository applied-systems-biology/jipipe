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

package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that passes the input to the output
 */
@ACAQDocumentation(name = "IO Interface", description = "Passes its input to its output without changes.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Miscellaneous)
public class IOInterfaceAlgorithm extends ACAQAlgorithm {

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link ACAQGraphNode}'s newInstance() method
     *
     * @param declaration The algorithm declaration
     */
    public IOInterfaceAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQIOSlotConfiguration());
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
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ACAQDataSlot outputSlot = getOutputSlot(inputSlot.getName());
            outputSlot.copyFrom(inputSlot);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    /**
     * Removes the interface and directly connects the inputs and outputs
     *
     * @param algorithm the algorithm
     */
    public static void collapse(IOInterfaceAlgorithm algorithm) {
        ACAQGraph graph = algorithm.getGraph();
        Map<String, ACAQDataSlot> inputSourceMap = new HashMap<>();
        Map<String, Set<ACAQDataSlot>> outputTargetMap = new HashMap<>();
        for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
            ACAQDataSlot sourceSlot = graph.getSourceSlot(inputSlot);
            if (sourceSlot != null) {
                inputSourceMap.put(inputSlot.getName(), sourceSlot);
            }
        }
        for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
            outputTargetMap.put(outputSlot.getName(), graph.getTargetSlots(outputSlot));
        }

        graph.removeNode(algorithm, false);

        for (Map.Entry<String, ACAQDataSlot> entry : inputSourceMap.entrySet()) {
            ACAQDataSlot source = entry.getValue();
            for (ACAQDataSlot target : outputTargetMap.getOrDefault(entry.getKey(), Collections.emptySet())) {
                graph.connect(source, target);
            }
        }

    }

    /**
     * Replaces a {@link ACAQCompartmentOutput} by an equivalent {@link IOInterfaceAlgorithm}
     *
     * @param compartmentOutput the output to be replaced
     */
    public static void replaceCompartmentOutput(ACAQCompartmentOutput compartmentOutput) {
        ACAQGraph graph = compartmentOutput.getGraph();
        String id = compartmentOutput.getIdInGraph();
        IOInterfaceAlgorithm ioInterfaceAlgorithm = ACAQAlgorithm.newInstance("io-interface");
        ioInterfaceAlgorithm.setCustomName(compartmentOutput.getName());
        ioInterfaceAlgorithm.setCustomDescription(compartmentOutput.getCustomDescription());
        ioInterfaceAlgorithm.getSlotConfiguration().setTo(compartmentOutput.getSlotConfiguration());

        Map<String, ACAQDataSlot> inputSourceMap = new HashMap<>();
        Map<String, Set<ACAQDataSlot>> outputTargetMap = new HashMap<>();
        for (ACAQDataSlot inputSlot : compartmentOutput.getInputSlots()) {
            ACAQDataSlot sourceSlot = graph.getSourceSlot(inputSlot);
            if (sourceSlot != null) {
                inputSourceMap.put(inputSlot.getName(), sourceSlot);
            }
        }
        for (ACAQDataSlot outputSlot : compartmentOutput.getOutputSlots()) {
            outputTargetMap.put(outputSlot.getName(), graph.getTargetSlots(outputSlot));
        }
        graph.removeNode(compartmentOutput, false);
        graph.insertNode(id, ioInterfaceAlgorithm, compartmentOutput.getCompartment());
        for (Map.Entry<String, ACAQDataSlot> entry : inputSourceMap.entrySet()) {
            ACAQDataSlot target = ioInterfaceAlgorithm.getInputSlot(entry.getKey());
            graph.connect(entry.getValue(), target);
        }
        for (Map.Entry<String, Set<ACAQDataSlot>> entry : outputTargetMap.entrySet()) {
            ACAQDataSlot source = ioInterfaceAlgorithm.getOutputSlot(entry.getKey());
            for (ACAQDataSlot target : entry.getValue()) {
                graph.connect(source, target);
            }
        }
    }
}
