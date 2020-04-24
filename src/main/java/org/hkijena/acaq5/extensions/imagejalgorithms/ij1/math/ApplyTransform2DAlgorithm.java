package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.math;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@ACAQDocumentation(name = "Apply transformation", description = "Applies a mathematical function to each pixel. ")
@ACAQOrganization(menuPath = "Math", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class ApplyTransform2DAlgorithm extends ImageJ1Algorithm {

    private Transformation transformation = Transformation.Absolute;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ApplyTransform2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public ApplyTransform2DAlgorithm(ApplyTransform2DAlgorithm other) {
        super(other);
        this.transformation = other.transformation;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachSlice(img, ip -> {
            switch (transformation) {
                case Absolute:
                    ip.abs();
                    break;
                case Invert:
                    ip.invert();
                    break;
                case Square:
                    ip.sqr();
                    break;
                case Logarithm:
                    ip.ln();
                    break;
                case SquareRoot:
                    ip.sqrt();
                    break;
                case Exponential:
                    ip.exp();
                    break;
            }
        });
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Function", description = "The function that is applied to each pixel.")
    @ACAQParameter("transformation-function")
    public Transformation getTransformation() {
        return transformation;
    }

    @ACAQParameter("transformation-function")
    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
    }

    /**
     * Available transformation functions
     */
    public enum Transformation {
        Absolute,
        Exponential,
        Invert,
        Logarithm,
        Square,
        SquareRoot
    }
}
