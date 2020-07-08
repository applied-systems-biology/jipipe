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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.plugin.StackReverser;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Invert 3D stack Z-order", description = "Inverts the order of a Z-stack.")
@ACAQOrganization(menuPath = "Dimensions", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlus3DData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlus3DData.class, slotName = "Output")
public class StackInverterAlgorithm extends ACAQSimpleIteratingAlgorithm {

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public StackInverterAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlus3DData.class)
                .addOutputSlot("Output", ImagePlus3DData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public StackInverterAlgorithm(StackInverterAlgorithm other) {
        super(other);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataInterface.getInputData(getFirstInputSlot(), ImagePlus3DData.class).getImage().duplicate();
        StackReverser reverser = new StackReverser();
        reverser.flipStack(img);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus3DData(img));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
