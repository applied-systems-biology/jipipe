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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.*;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Flip 2D image", description = "Flips the image vertical or horizontal. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@DefineJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nTransform", aliasName = "Flip")
public class TransformFlip2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private FlipMode flipMode = FlipMode.Horizontal;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TransformFlip2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
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
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Flip direction", description = "The direction to flip")
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
