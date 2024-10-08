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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.dimensions;

import ij.ImagePlus;
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
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.Reslicer;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.plugins.parameters.library.roi.AnchorParameterSettings;

@SetJIPipeDocumentation(name = "Reslice", description = "Defines a new Z axis and projects the image so that its Z axis is the newly defined one.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks", aliasName = "Reslice Z")
public class ResliceAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Anchor planeStartLocation = Anchor.TopCenter;
    private boolean noInterpolation = true;
    private boolean flipVertical = false;
    private boolean rotate = false;

    public ResliceAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ResliceAlgorithm(ResliceAlgorithm other) {
        super(other);
        this.planeStartLocation = other.planeStartLocation;
        this.noInterpolation = other.noInterpolation;
        this.flipVertical = other.flipVertical;
        this.rotate = other.rotate;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        Reslicer reslicer = new Reslicer();
        reslicer.setFlip(flipVertical);
        reslicer.setRotate(rotate);
        switch (planeStartLocation) {
            case TopCenter:
                reslicer.setStartAt(Reslicer.Direction.Top);
                break;
            case CenterLeft:
                reslicer.setStartAt(Reslicer.Direction.Left);
                break;
            case CenterRight:
                reslicer.setStartAt(Reslicer.Direction.Right);
                break;
            case BottomCenter:
                reslicer.setStartAt(Reslicer.Direction.Bottom);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported plane start location: " + planeStartLocation);
        }
        ImagePlus result = reslicer.reslice(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Flip vertically")
    @JIPipeParameter("flip-vertical")
    public boolean isFlipVertical() {
        return flipVertical;
    }

    @JIPipeParameter("flip-vertical")
    public void setFlipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
    }

    @SetJIPipeDocumentation(name = "Rotate 90 degrees")
    @JIPipeParameter("rotate")
    public boolean isRotate() {
        return rotate;
    }

    @JIPipeParameter("rotate")
    public void setRotate(boolean rotate) {
        this.rotate = rotate;
    }

    @SetJIPipeDocumentation(name = "Avoid interpolation", description = "If enabled, use 1 pixel spacing if possible.")
    @JIPipeParameter("no-interpolation")
    public boolean isNoInterpolation() {
        return noInterpolation;
    }

    @JIPipeParameter("no-interpolation")
    public void setNoInterpolation(boolean noInterpolation) {
        this.noInterpolation = noInterpolation;
    }

    @SetJIPipeDocumentation(name = "Start at", description = "Determines where the reslice will start. For example, if you choose the top location, the reslice plane will wander from north to south.")
    @JIPipeParameter(value = "plane-start-location", important = true)
    @AnchorParameterSettings(allowTopLeft = false, allowTopRight = false, allowCenterCenter = false, allowBottomLeft = false, allowBottomRight = false)
    public Anchor getPlaneStartLocation() {
        return planeStartLocation;
    }

    @JIPipeParameter("plane-start-location")
    public void setPlaneStartLocation(Anchor planeStartLocation) {
        this.planeStartLocation = planeStartLocation;
    }
}
