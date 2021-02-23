package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Warp 2D", description = "Warps the image by applying a 2-channel vector field of relative coordinates where the pixels should be copied to." +
        " The vector field should either have the " +
        "same dimensions as the input, or consist of only one plane, where it will be applied to all input planes.")
@JIPipeOrganization(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Vector field", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Image")
public class Warp2DAlgorithm extends JIPipeIteratingAlgorithm {

    private HyperstackDimension vectorDimension = HyperstackDimension.Channel;
    private boolean invertTransform = false;
    private boolean polarCoordinates = false;
    private boolean absoluteCoordinates = false;

    public Warp2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Warp2DAlgorithm(Warp2DAlgorithm other) {
        super(other);
        this.vectorDimension = other.vectorDimension;
        this.invertTransform = other.invertTransform;
        this.polarCoordinates = other.polarCoordinates;
        this.absoluteCoordinates = other.absoluteCoordinates;
    }

    @JIPipeDocumentation(name = "Vector dimension", description = "Determines which dimension stores the vector coordinates. " +
            "This dimension will have the same vector field across all slices of it. The vector dimension is ignored if the vector field has exactly two " +
            "planes. Here, the same field is applied to all slices.")
    @JIPipeParameter("vector-dimension")
    public HyperstackDimension getVectorDimension() {
        return vectorDimension;
    }

    @JIPipeParameter("vector-dimension")
    public void setVectorDimension(HyperstackDimension vectorDimension) {
        this.vectorDimension = vectorDimension;
    }

    @JIPipeDocumentation(name = "Invert transform", description = "If enabled, the transform is reversed.")
    @JIPipeParameter("invert-transform")
    public boolean isInvertTransform() {
        return invertTransform;
    }

    @JIPipeParameter("invert-transform")
    public void setInvertTransform(boolean invertTransform) {
        this.invertTransform = invertTransform;
    }

    @JIPipeDocumentation(name = "Use polar coordinates", description = "If enabled, the vector field is assumed to be provided in polar coordinates (r, phi) instead of (x, y).")
    @JIPipeParameter("polar-coordinates")
    public boolean isPolarCoordinates() {
        return polarCoordinates;
    }

    @JIPipeParameter("polar-coordinates")
    public void setPolarCoordinates(boolean polarCoordinates) {
        this.polarCoordinates = polarCoordinates;
    }

    @JIPipeDocumentation(name = "Absolute coordinates", description = "If enabled, the vector field is assumed to have absolute coordinates. " +
            "For polar coordinates, the origin is assumed to be (0, 0).")
    @JIPipeParameter("absolute-coordinates")
    public boolean isAbsoluteCoordinates() {
        return absoluteCoordinates;
    }

    @JIPipeParameter("absolute-coordinates")
    public void setAbsoluteCoordinates(boolean absoluteCoordinates) {
        this.absoluteCoordinates = absoluteCoordinates;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        ImagePlus vectorField = dataBatch.getInputData("Vector field", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus result = IJ.createHyperStack(img.getTitle() + " warped",
                img.getWidth(),
                img.getHeight(),
                img.getNChannels(),
                img.getNSlices(),
                img.getNFrames(),
                img.getBitDepth());

        boolean globalVectorField = vectorField.getStackSize() == 2;
        if(!globalVectorField) {
            int vectorChannels;
            switch (vectorDimension) {
                case Channel:
                    vectorChannels = vectorField.getNChannels();
                    break;
                case Depth:
                    vectorChannels = vectorField.getNSlices();
                    break;
                case Frame:
                    vectorChannels = vectorField.getNFrames();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            if(vectorChannels != 2) {
                throw new UserFriendlyRuntimeException("Vector field has wrong number of slices!",
                        "Invalid vector field!",
                        getName(),
                        "The vector field for warping must have exactly two slices in the selected vector dimension or only two slices at all!",
                        "Please provide a valid vector field.");
            }
        }

        ImageJUtils.forEachIndexedZCTSlice(img, (inputProcessor, index) -> {
            // Get the result processor
            ImageProcessor resultProcessor;
            if(result.isStack())
                resultProcessor = result.getStack().getProcessor(result.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1));
            else
                resultProcessor = result.getProcessor();

            // Get vector processors
            ImageProcessor vecX;
            ImageProcessor vecY;

            if(globalVectorField) {
                vecX = vectorField.getStack().getProcessor(1);
                vecY = vectorField.getStack().getProcessor(2);
            }
            else {
                switch (vectorDimension) {
                    case Channel:
                        vecX = vectorField.getStack().getProcessor(vectorField.getStackIndex(1, index.getZ() + 1, index.getT() + 1));
                        vecY = vectorField.getStack().getProcessor(vectorField.getStackIndex(2, index.getZ() + 1, index.getT() + 1));
                        break;
                    case Depth:
                        vecX = vectorField.getStack().getProcessor(vectorField.getStackIndex(index.getC() + 1, 1, index.getT() + 1));
                        vecY = vectorField.getStack().getProcessor(vectorField.getStackIndex(index.getC() + 1, 2, index.getT() + 1));
                        break;
                    case Frame:
                        vecX = vectorField.getStack().getProcessor(vectorField.getStackIndex(index.getC() + 1, index.getZ() + 1, 1));
                        vecY = vectorField.getStack().getProcessor(vectorField.getStackIndex(index.getC() + 1, index.getZ() + 1, 2));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            for (int y = 0; y < inputProcessor.getHeight(); y++) {
                for (int x = 0; x < inputProcessor.getWidth(); x++) {
                    double dx = vecX.getf(x, y);
                    double dy = vecY.getf(x, y);

                    int sx = x;
                    int sy = y;
                    int tx;
                    int ty;

                    if(polarCoordinates) {
                        tx = (int)(dx * Math.cos(dy));
                        ty = (int)(dy * Math.sin(dy));
                        if(!absoluteCoordinates) {
                            tx += x;
                            ty += y;
                        }
                    }
                    else {
                        if(absoluteCoordinates) {
                            tx = (int)dx;
                            ty = (int)dy;
                        }
                        else {
                            tx = (int) (x + dx);
                            ty = (int)(y + dy);
                        }
                    }

                    if(invertTransform) {
                        int _sx = sx;
                        int _sy = sy;
                        sx = tx;
                        sy = ty;
                        tx = _sx;
                        ty = _sy;
                    }

                    // Correct source position
                    while(sx < 0) {
                        sx += inputProcessor.getWidth();
                    }
                    while(sy < 0) {
                        sy += inputProcessor.getHeight();
                    }
                    sx = sx % inputProcessor.getWidth();
                    sy = sy % inputProcessor.getHeight();

                    // Correct target position
                    while(tx < 0) {
                        tx += inputProcessor.getWidth();
                    }
                    while(ty < 0) {
                        ty += inputProcessor.getHeight();
                    }
                    tx = tx % inputProcessor.getWidth();
                    ty = ty % inputProcessor.getHeight();

                    // Copy pixel
                    resultProcessor.set(tx, ty, inputProcessor.get(sx, sy));
                }
            }
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }
}
