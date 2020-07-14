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
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Rotate 2D image", description = "Rotates the image in 90° steps to the left or to the right. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Transform", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformRotate2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private RotationMode rotationDirection = RotationMode.Right;
    private int rotations = 1;

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public TransformRotate2DAlgorithm(JIPipeNodeInfo info) {
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
    public TransformRotate2DAlgorithm(TransformRotate2DAlgorithm other) {
        super(other);
        this.rotationDirection = other.rotationDirection;
        this.rotations = other.rotations;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus imp = inputData.getImage().duplicate();
        for (int i = 0; i < rotations; ++i) {
            ImageJUtils.forEachSlice(imp, ip -> {
                Calibration cal = imp.getCalibration();
                boolean transformOrigin = cal.xOrigin != 0 || cal.yOrigin != 0;
                StackProcessor sp = new StackProcessor(imp.getStack(), ip);
                ImageStack s2 = null;
                if (rotationDirection == RotationMode.Right) {
                    s2 = sp.rotateRight();
                    if (transformOrigin) {
                        double xOrigin = imp.getHeight() - 1 - cal.yOrigin;
                        double yOrigin = cal.xOrigin;
                        cal.xOrigin = xOrigin;
                        cal.yOrigin = yOrigin;
                    }
                } else {
                    s2 = sp.rotateLeft();
                    if (transformOrigin) {
                        double xOrigin = cal.yOrigin;
                        double yOrigin = imp.getWidth() - 1 - cal.xOrigin;
                        cal.xOrigin = xOrigin;
                        cal.yOrigin = yOrigin;
                    }
                }
                imp.setStack(null, s2);
                double pixelWidth = cal.pixelWidth;
                cal.pixelWidth = cal.pixelHeight;
                cal.pixelHeight = pixelWidth;
                if (!cal.getXUnit().equals(cal.getYUnit())) {
                    String xUnit = cal.getXUnit();
                    cal.setXUnit(cal.getYUnit());
                    cal.setYUnit(xUnit);
                }
            });
        }
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(imp));
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Number of rotations").checkIfWithin(this, rotations, 1, Double.POSITIVE_INFINITY, true, true);
    }

    @JIPipeDocumentation(name = "Rotation direction", description = "The direction to rotate")
    @JIPipeParameter("rotation-direction")
    public RotationMode getRotationDirection() {
        return rotationDirection;
    }

    @JIPipeParameter("rotation-direction")
    public void setRotationDirection(RotationMode rotationDirection) {
        this.rotationDirection = rotationDirection;
    }

    @JIPipeDocumentation(name = "Number of rotations", description = "How many times the image is rotated")
    @JIPipeParameter("num-rotations")
    public int getRotations() {
        return rotations;
    }

    @JIPipeParameter("num-rotations")
    public void setRotations(int rotations) {
        this.rotations = rotations;
    }

    /**
     * Available ways to flip an image
     */
    public enum RotationMode {
        Left,
        Right
    }
}
