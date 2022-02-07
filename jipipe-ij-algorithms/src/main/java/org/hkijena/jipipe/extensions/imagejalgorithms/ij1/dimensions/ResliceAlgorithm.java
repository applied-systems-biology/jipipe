package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.Reslicer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.extensions.parameters.library.roi.AnchorParameterSettings;

@JIPipeDocumentation(name = "Reslice", description = "Defines a new Z axis and projects the image so that its Z axis is the newly defined one.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
@JIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
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
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "Flip vertically")
    @JIPipeParameter("flip-vertical")
    public boolean isFlipVertical() {
        return flipVertical;
    }

    @JIPipeParameter("flip-vertical")
    public void setFlipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
    }

    @JIPipeDocumentation(name = "Rotate 90 degrees")
    @JIPipeParameter("rotate")
    public boolean isRotate() {
        return rotate;
    }

    @JIPipeParameter("rotate")
    public void setRotate(boolean rotate) {
        this.rotate = rotate;
    }

    @JIPipeDocumentation(name = "Avoid interpolation", description = "If enabled, use 1 pixel spacing if possible.")
    @JIPipeParameter("no-interpolation")
    public boolean isNoInterpolation() {
        return noInterpolation;
    }

    @JIPipeParameter("no-interpolation")
    public void setNoInterpolation(boolean noInterpolation) {
        this.noInterpolation = noInterpolation;
    }

    @JIPipeDocumentation(name = "Start at", description = "Determines where the reslice will start. For example, if you choose the top location, the reslice plane will wander from north to south.")
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
