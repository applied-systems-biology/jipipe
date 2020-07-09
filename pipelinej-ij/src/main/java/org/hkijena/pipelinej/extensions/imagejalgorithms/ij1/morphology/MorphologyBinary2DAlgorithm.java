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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.morphology;

import ij.ImagePlus;
import ij.Prefs;
import ij.process.ByteProcessor;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.pipelinej.extensions.imagejalgorithms.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@ACAQDocumentation(name = "Morphological operation (binary) 2D", description = "Applies a morphological operation to binary images." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Morphology", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class MorphologyBinary2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private Operation operation = Operation.Dilate;
    private int iterations = 1;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public MorphologyBinary2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscaleMaskData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public MorphologyBinary2DAlgorithm(MorphologyBinary2DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.iterations = other.iterations;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachSlice(img, ip -> {
            int fg = Prefs.blackBackground ? 255 : 0;
            int foreground = ip.isInvertedLut() ? 255 - fg : fg;
            int background = 255 - foreground;
            switch (operation) {
                case Dilate:
                    ((ByteProcessor) ip).dilate(iterations, background);
                    break;
                case Erode:
                    ((ByteProcessor) ip).erode(iterations, background);
                    break;
                case Open:
                    ((ByteProcessor) ip).erode(iterations, background);
                    ((ByteProcessor) ip).dilate(iterations, background);
                    break;
                case Close:
                    ((ByteProcessor) ip).dilate(iterations, background);
                    ((ByteProcessor) ip).erode(iterations, background);
                    break;
            }
        });
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Iterations").checkIfWithin(this, iterations, 1, Integer.MAX_VALUE, true, true);
    }

    @ACAQDocumentation(name = "Operation", description = "The morphological operation")
    @ACAQParameter("operation")
    public Operation getOperation() {
        return operation;
    }

    @ACAQParameter("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;

    }

    @ACAQDocumentation(name = "Iterations", description = "How many times the operation is applied")
    @ACAQParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @ACAQParameter("iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;

    }

    /**
     * Available transformation functions
     */
    public enum Operation {
        Erode, Dilate, Open, Close
    }
}
