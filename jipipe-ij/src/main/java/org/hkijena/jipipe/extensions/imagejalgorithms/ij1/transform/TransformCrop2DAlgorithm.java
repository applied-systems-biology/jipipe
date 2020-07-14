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
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Crop 2D image", description = "Crops a 2D image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Transform", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformCrop2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Margin roi = new Margin();

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public TransformCrop2DAlgorithm(JIPipeNodeInfo info) {
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
    public TransformCrop2DAlgorithm(TransformCrop2DAlgorithm other) {
        super(other);
        this.roi = new Margin(other.roi);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage();

        Rectangle imageArea = new Rectangle(0, 0, img.getWidth(), img.getHeight());
        Rectangle cropped = roi.apply(imageArea);
        if (cropped == null || cropped.width == 0 || cropped.height == 0) {
            throw new UserFriendlyRuntimeException(new NullPointerException("Cropped rectangle is null or empty!"),
                    "Cropped rectangle is empty!",
                    "Algorithm '" + getName() + "'",
                    "The input for the cropping operator was an image of size w=" + img.getWidth() + ", h=" + img.getHeight() + ". The resulting ROI is empty.",
                    "Please check the parameters and ensure that a non-empty area is cropped out.");
        }
//        if(!imageArea.inter(cropped)) {
//            throw new UserFriendlyRuntimeException(new NullPointerException("Cropped rectangle is outside of image!"),
//                    "Cropped rectangle is outside of the image!",
//                    "Algorithm '" +getName() + "'",
//                    "The input for the cropping operator was an image of size w=" + img.getWidth() + ", h=" + img.getHeight() + ". The resulting ROI is a rectangle with following properties: " +
//                            cropped + ". The rectangle is outside of the image dimensions.",
//                    "Please check the parameters and ensure that you only crop ");
//        }

        if (img.isStack()) {
            ImageStack result = new ImageStack(cropped.width, cropped.height, img.getProcessor().getColorModel());
            ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
                imp.setRoi(cropped);
                ImageProcessor croppedImage = img.getProcessor().crop();
                imp.resetRoi();
                result.addSlice("" + index, croppedImage);
            });
            dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Cropped", result)));
        } else {
            ImageProcessor imp = img.getProcessor();
            imp.setRoi(cropped);
            ImageProcessor croppedImage = imp.crop();
            imp.resetRoi();
            ImagePlus result = new ImagePlus("Cropped", croppedImage);
            dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
        }
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "ROI", description = "Defines the area to crop.")
    @JIPipeParameter("roi")
    public Margin getRoi() {
        return roi;
    }

    @JIPipeParameter("roi")
    public void setRoi(Margin roi) {
        this.roi = roi;

    }
}
