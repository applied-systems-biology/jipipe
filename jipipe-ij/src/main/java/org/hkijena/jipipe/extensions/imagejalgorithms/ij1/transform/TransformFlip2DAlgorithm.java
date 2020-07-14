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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Flip 2D image", description = "Flips the image vertical or horizontal. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Transform", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformFlip2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private FlipMode flipMode = FlipMode.Horizontal;

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public TransformFlip2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getDuplicateImage();
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
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Flip direction", description = "The direction to flip")
    @JIPipeParameter("flip-mode")
    public FlipMode getFlipMode() {
        return flipMode;
    }

    @JIPipeParameter("flip-mode")
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
