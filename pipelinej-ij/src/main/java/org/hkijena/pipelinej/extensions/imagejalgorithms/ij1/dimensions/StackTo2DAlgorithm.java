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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.pipelinej.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.pipelinej.utils.ResourceUtils;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_2D_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Split stack into 2D", description = "Splits high-dimensional image stacks into 2D planes")
@ACAQOrganization(menuPath = "Dimensions", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlus2DData.class, slotName = "Output")
public class StackTo2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private boolean annotateSlices = true;
    private String annotationType = "Image index";

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public StackTo2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
        this.annotationType = other.annotationType;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachIndexedSlice(img, (ip, index) -> {
            if (annotateSlices) {
                ACAQAnnotation trait = new ACAQAnnotation(annotationType, "slice=" + index);
                dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DData(new ImagePlus("slice=" + index, ip)), Collections.singletonList(trait));
            } else {
                dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DData(new ImagePlus("slice=" + index, ip)));
            }
        });
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (annotateSlices) {
            report.forCategory("Generated annotation").checkNonEmpty(annotationType, this);
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
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }
}
