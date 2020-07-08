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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Flip 2D image", description = "Flips the image vertical or horizontal. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Transform", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformFlip2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private FlipMode flipMode = FlipMode.Horizontal;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public TransformFlip2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public TransformFlip2DAlgorithm(TransformFlip2DAlgorithm other) {
        super(other);
        this.flipMode = other.flipMode;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        boolean fliph = flipMode == FlipMode.Horizontal || flipMode == FlipMode.Both;
        boolean flipv = flipMode == FlipMode.Vertical || flipMode == FlipMode.Both;
        ImageJUtils.forEachSlice(img, ip -> {
            Calibration cal = img.getCalibration();
            boolean transformOrigin = cal.xOrigin != 0 || cal.yOrigin != 0;
            if (fliph) {
                ip.flipHorizontal();
                Rectangle r = ip.getRoi();
                if (transformOrigin && r.x == 0 && r.y == 0 && r.width == ip.getWidth() && r.height == ip.getHeight())
                    cal.xOrigin = img.getWidth() - 1 - cal.xOrigin;
            }
            if (flipv) {
                ip.flipVertical();
                Rectangle r = ip.getRoi();
                if (transformOrigin && r.x == 0 && r.y == 0 && r.width == ip.getWidth() && r.height == ip.getHeight())
                    cal.yOrigin = img.getHeight() - 1 - cal.yOrigin;
            }
        });
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Flip direction", description = "The direction to flip")
    @ACAQParameter("flip-mode")
    public FlipMode getFlipMode() {
        return flipMode;
    }

    @ACAQParameter("flip-mode")
    public void setFlipMode(FlipMode flipMode) {
        this.flipMode = flipMode;
    }

    /**
     * Available ways to flip an image
     */
    public enum FlipMode {
        Vertical,
        Horizontal,
        Both
    }
}
