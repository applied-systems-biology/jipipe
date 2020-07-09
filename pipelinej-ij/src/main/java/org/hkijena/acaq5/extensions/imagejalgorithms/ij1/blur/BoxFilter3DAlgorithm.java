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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Filters3D;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link GaussianBlur}
 */
@ACAQDocumentation(name = "Box filter 3D", description = "Applies convolution with a local mean function in 3D space for smoothing. " +
        "If higher-dimensional data is provided, the filter is applied to each 3D slice.")
@ACAQOrganization(menuPath = "Blur", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class BoxFilter3DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private float radiusX = 2;
    private float radiusY = -1;
    private float radiusZ = -1;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public BoxFilter3DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
    public BoxFilter3DAlgorithm(BoxFilter3DAlgorithm other) {
        super(other);
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
        this.radiusZ = other.radiusZ;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageStack filtered = Filters3D.filter(img.getStack(), Filters3D.MEAN, radiusX, radiusY <= 0 ? radiusX : radiusY, radiusZ <= 0 ? radiusX : radiusZ);
        ImagePlus result = new ImagePlus("Output", filtered);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Radius (X)").checkIfWithin(this, radiusX, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @ACAQDocumentation(name = "Radius (X)", description = "Filter radius (pixels) in X direction. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @ACAQParameter("radius-x")
    public float getRadiusX() {
        return radiusX;
    }

    @ACAQParameter("radius-x")
    public void setRadiusX(float radiusX) {
        this.radiusX = radiusX;

    }

    @ACAQDocumentation(name = "Radius (Y)", description = "Filter radius (pixels) in Y direction." +
            " If zero or less, radius in X direction is automatically used instead. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @ACAQParameter("radius-y")
    public float getRadiusY() {
        return radiusY;
    }

    @ACAQParameter("radius-y")
    public void setRadiusY(float radiusY) {
        this.radiusY = radiusY;

    }

    @ACAQDocumentation(name = "Radius (Z)", description = "Filter radius (pixels) in Z direction." +
            " If zero or less, radius in X direction is automatically used instead. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @ACAQParameter("radius-z")
    public float getRadiusZ() {
        return radiusZ;
    }

    @ACAQParameter("radius-z")
    public void setRadiusZ(float radiusZ) {
        this.radiusZ = radiusZ;

    }
}
