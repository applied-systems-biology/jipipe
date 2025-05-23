/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Rotate 2D image", description = "Rotates the image in 90° steps to the left or to the right. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nTransform", aliasName = "Rotate (90 deg steps)")
public class TransformRotate2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private RotationMode rotationDirection = RotationMode.Right;
    private int rotations = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TransformRotate2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus imp = inputData.getDuplicateImage();
        for (int i = 0; i < rotations; ++i) {
            ImageJIterationUtils.forEachSlice(imp, ip -> {
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
            }, progressInfo);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(imp), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Rotation direction", description = "The direction to rotate")
    @JIPipeParameter("rotation-direction")
    public RotationMode getRotationDirection() {
        return rotationDirection;
    }

    @JIPipeParameter("rotation-direction")
    public void setRotationDirection(RotationMode rotationDirection) {
        this.rotationDirection = rotationDirection;
    }

    @SetJIPipeDocumentation(name = "Number of rotations", description = "How many times the image is rotated")
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
