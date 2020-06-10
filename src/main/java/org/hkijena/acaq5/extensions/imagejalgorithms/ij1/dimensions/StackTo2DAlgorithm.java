package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.acaq5.extensions.parameters.editors.ACAQTraitParameterSettings;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_2D_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Split stack into 2D", description = "Splits high-dimensional image stacks into 2D planes")
@ACAQOrganization(menuPath = "Dimensions", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlus2DData.class, slotName = "Output")
public class StackTo2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private boolean annotateSlices = true;
    private ACAQTraitDeclarationRef annotationType = new ACAQTraitDeclarationRef(ACAQTraitRegistry.getInstance().getDeclarationById("image-index"));

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public StackTo2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlus2DData.class, "Input", TO_2D_CONVERSION)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public StackTo2DAlgorithm(StackTo2DAlgorithm other) {
        super(other);
        this.annotateSlices = other.annotateSlices;
        this.annotationType = new ACAQTraitDeclarationRef(other.annotationType);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachIndexedSlice(img, (ip, index) -> {
            if (annotateSlices) {
                ACAQTrait trait = annotationType.getDeclaration().newInstance("slice=" + index);
                dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DData(new ImagePlus("slice=" + index, ip)), Collections.singletonList(trait));
            } else {
                dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DData(new ImagePlus("slice=" + index, ip)));
            }
        });
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (annotateSlices) {
            if (annotationType != null) {
                report.forCategory("Generated annotation").checkNonNull(annotationType.getDeclaration(), this);
            } else {
                report.forCategory("Generated annotation").checkNonNull(annotationType, this);
            }
        }
    }

    @ACAQDocumentation(name = "Annotate slices", description = "Annotates each generated image slice.")
    @ACAQParameter("annotate-slices")
    public boolean isAnnotateSlices() {
        return annotateSlices;
    }

    @ACAQParameter("annotate-slices")
    public void setAnnotateSlices(boolean annotateSlices) {
        this.annotateSlices = annotateSlices;
    }

    @ACAQDocumentation(name = "Generated annotation", description = "Determines the generated annotation type.")
    @ACAQParameter("annotation-type")
    public ACAQTraitDeclarationRef getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(ACAQTraitDeclarationRef annotationType) {
        this.annotationType = annotationType;
    }
}
