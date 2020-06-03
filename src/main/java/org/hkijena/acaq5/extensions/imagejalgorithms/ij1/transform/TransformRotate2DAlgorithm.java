package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
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

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Rotate 2D image", description = "Rotates the image in 90° steps to the left or to the right. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Transform", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformRotate2DAlgorithm extends ImageJ1Algorithm {

    private RotationMode rotationDirection = RotationMode.Right;
    private int rotations = 1;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public TransformRotate2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Number of rotations").checkIfWithin(this, rotations, 1, Double.POSITIVE_INFINITY, true, true);
    }

    @ACAQDocumentation(name = "Rotation direction", description = "The direction to rotate")
    @ACAQParameter("rotation-direction")
    public RotationMode getRotationDirection() {
        return rotationDirection;
    }

    @ACAQParameter("rotation-direction")
    public void setRotationDirection(RotationMode rotationDirection) {
        this.rotationDirection = rotationDirection;
    }

    @ACAQDocumentation(name = "Number of rotations", description = "How many times the image is rotated")
    @ACAQParameter("num-rotations")
    public int getRotations() {
        return rotations;
    }

    @ACAQParameter("num-rotations")
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
