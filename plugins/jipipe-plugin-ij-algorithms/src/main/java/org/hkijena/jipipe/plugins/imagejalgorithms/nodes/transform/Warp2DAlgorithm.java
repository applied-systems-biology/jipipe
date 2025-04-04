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

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;

@SetJIPipeDocumentation(name = "Warp 2D", description = "Warps the image by applying a 2-channel vector field of relative coordinates where the pixels should be copied to." +
        " The vector field should either have the " +
        "same dimensions as the input, or consist of only one plane, where it will be applied to all input planes.")
@ConfigureJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, name = "Vector field", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class Warp2DAlgorithm extends JIPipeIteratingAlgorithm {

    private HyperstackDimension vectorDimension = HyperstackDimension.Channel;
    private boolean invertTransform = false;
    private boolean polarCoordinates = false;
    private boolean absoluteCoordinates = false;
    private double multiplier = 1.0;
    private WrapMode wrapMode = WrapMode.None;

    public Warp2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Warp2DAlgorithm(Warp2DAlgorithm other) {
        super(other);
        this.vectorDimension = other.vectorDimension;
        this.invertTransform = other.invertTransform;
        this.polarCoordinates = other.polarCoordinates;
        this.absoluteCoordinates = other.absoluteCoordinates;
        this.wrapMode = other.wrapMode;
        this.multiplier = other.multiplier;
    }

    @SetJIPipeDocumentation(name = "Vector dimension", description = "Determines which dimension stores the vector coordinates. " +
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

    @SetJIPipeDocumentation(name = "Invert transform", description = "If enabled, the transform is reversed.")
    @JIPipeParameter("invert-transform")
    public boolean isInvertTransform() {
        return invertTransform;
    }

    @JIPipeParameter("invert-transform")
    public void setInvertTransform(boolean invertTransform) {
        this.invertTransform = invertTransform;
    }

    @SetJIPipeDocumentation(name = "Use polar coordinates", description = "If enabled, the vector field is assumed to be provided in polar coordinates (r, phi) instead of (x, y).")
    @JIPipeParameter("polar-coordinates")
    public boolean isPolarCoordinates() {
        return polarCoordinates;
    }

    @JIPipeParameter("polar-coordinates")
    public void setPolarCoordinates(boolean polarCoordinates) {
        this.polarCoordinates = polarCoordinates;
    }

    @SetJIPipeDocumentation(name = "Absolute coordinates", description = "If enabled, the vector field is assumed to have absolute coordinates. " +
            "For polar coordinates, the origin is assumed to be (0, 0).")
    @JIPipeParameter("absolute-coordinates")
    public boolean isAbsoluteCoordinates() {
        return absoluteCoordinates;
    }

    @JIPipeParameter("absolute-coordinates")
    public void setAbsoluteCoordinates(boolean absoluteCoordinates) {
        this.absoluteCoordinates = absoluteCoordinates;
    }

    @SetJIPipeDocumentation(name = "Wrap mode", description = "Determines what to do with source/target pixels that are outside the image. 'None' means that the pixel is skipped.")
    @JIPipeParameter("wrap-mode")
    public WrapMode getWrapMode() {
        return wrapMode;
    }

    @JIPipeParameter("wrap-mode")
    public void setWrapMode(WrapMode wrapMode) {
        this.wrapMode = wrapMode;
    }

    @SetJIPipeDocumentation(name = "Transform multiplier", description = "Determines the relative amount of the transform. Zero means that no transform is applied. One (default) that " +
            "the transform is applied without any additional changes.")
    @JIPipeParameter("multiplier")
    public double getMultiplier() {
        return multiplier;
    }

    @JIPipeParameter("multiplier")
    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        ImagePlus vectorField = iterationStep.getInputData("Vector field", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus result = IJ.createHyperStack(img.getTitle() + " warped",
                img.getWidth(),
                img.getHeight(),
                img.getNChannels(),
                img.getNSlices(),
                img.getNFrames(),
                img.getBitDepth());

        boolean globalVectorField = vectorField.getStackSize() == 2;
        if (!globalVectorField) {
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
            if (vectorChannels != 2) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new GraphNodeValidationReportContext(this),
                        "Vector field has wrong number of slices!",
                        "The vector field for warping must have exactly two slices in the selected vector dimension or only two slices at all!",
                        "Please provide a valid vector field."));
            }
        }

        ImageJIterationUtils.forEachIndexedZCTSlice(img, (inputProcessor, index) -> {
            // Get the result processor
            ImageProcessor resultProcessor;
            if (result.hasImageStack())
                resultProcessor = result.getStack().getProcessor(result.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1));
            else
                resultProcessor = result.getProcessor();

            // Get vector processors
            ImageProcessor vecX;
            ImageProcessor vecY;

            if (globalVectorField) {
                vecX = vectorField.getStack().getProcessor(1);
                vecY = vectorField.getStack().getProcessor(2);
            } else {
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

                    if (polarCoordinates) {
                        tx = (int) (dx * Math.cos(dy));
                        ty = (int) (dy * Math.sin(dy));
                        if (!absoluteCoordinates) {
                            tx += x;
                            ty += y;
                        }
                    } else {
                        if (absoluteCoordinates) {
                            tx = (int) dx;
                            ty = (int) dy;
                        } else {
                            tx = (int) (x + dx);
                            ty = (int) (y + dy);
                        }
                    }

                    if (invertTransform) {
                        int _sx = sx;
                        int _sy = sy;
                        sx = tx;
                        sy = ty;
                        tx = _sx;
                        ty = _sy;
                    }

                    if (multiplier == 0.0) {
                        tx = sx;
                        ty = sy;
                    } else if (multiplier != 1.0) {
                        tx = (int) (sx + multiplier * (tx - sx));
                        ty = (int) (sy + multiplier * (ty - sy));
                    }

                    sx = wrapMode.wrap(sx, 0, inputProcessor.getWidth());
                    sy = wrapMode.wrap(sy, 0, inputProcessor.getHeight());
                    tx = wrapMode.wrap(tx, 0, resultProcessor.getWidth());
                    ty = wrapMode.wrap(ty, 0, resultProcessor.getHeight());

                    if (sx < 0 || sx >= inputProcessor.getWidth())
                        continue;
                    if (sy < 0 || sy >= inputProcessor.getHeight())
                        continue;
                    if (tx < 0 || tx >= resultProcessor.getWidth())
                        continue;
                    if (ty < 0 || ty >= resultProcessor.getHeight())
                        continue;

                    // Copy pixel
                    resultProcessor.set(tx, ty, inputProcessor.get(sx, sy));
                }
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }
}
