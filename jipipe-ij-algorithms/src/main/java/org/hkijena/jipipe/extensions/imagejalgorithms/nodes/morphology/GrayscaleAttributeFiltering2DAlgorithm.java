package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.morphology;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import inra.ijpb.algo.DefaultAlgoListener;
import inra.ijpb.morphology.attrfilt.AreaOpeningQueue;
import inra.ijpb.morphology.attrfilt.BoxDiagonalOpeningQueue;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Grayscale attribute filtering 2D", description = "Attribute filters aim at removing components of an image based on a certain size criterion, rather than on intensity. " +
        "The most common and useful criterion is the number of pixels/voxels (i.e., the area or volume). " +
        "For example, a morphological size opening operation with a threshold value of 20 will remove all blobs containing fewer than 20 voxels. " +
        "The length of the diagonal of the bounding box can also be of interest to discriminate elongated versus round component shapes.\n\n" +
        "<h2>Application to binary images</h2>\n\n" +
        "When applied to a binary image, attribute opening consists in identifying each connected component, computing the attribute measurement of each component, and retain only the connected components whose measurement is above a specified value. This kind of processing is often used to clean-up segmentation results.\n\n" +
        "<h2>Application to grayscale images</h2>\n\n" +
        "When applied to a grayscale image, attribute opening consists in generating a series of binary images by thresholding at each distinct gray level in the image. The binary attribute opening described above is then applied independently to each binary image and the grayscale output is computed as the union of the binary results. The final output is a grayscale image whose bright structures with the attribute below a given value have disappeared. A great advantage of this filter is that the contours of the structures area better preserved than opening with a structuring element.\n\n" +
        "As for classical morphological filters, grayscale attribute closing or tophat can be defined. Grayscale attribute closing consists in removing dark connected components whose size is smaller than a specified value. White [resp. Black] Attribute Top-Hat considers the difference of the attribute opening [resp. closing] with the original image, and can help identifying bright [resp. dark] structures with small size.")
@AddJIPipeCitation("More information here: https://imagej.net/plugins/morpholibj#attribute-filtering")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Morphology")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nFiltering\nGray Scale Attribute Filtering")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
public class GrayscaleAttributeFiltering2DAlgorithm extends JIPipeIteratingAlgorithm {

    private Operation operation = Operation.Opening;
    private Attribute attribute = Attribute.Area;

    private int minimumValue = 100;
    private Neighborhood2D connectivity = Neighborhood2D.FourConnected;

    public GrayscaleAttributeFiltering2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public GrayscaleAttributeFiltering2DAlgorithm(GrayscaleAttributeFiltering2DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.attribute = other.attribute;
        this.minimumValue = other.minimumValue;
        this.connectivity = other.connectivity;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImg = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImg = ImageJUtils.generateForEachIndexedZCTSlice(inputImg, (image, index) -> {
            ImageProcessor result;

            // Identify image to process (original, or inverted)
            ImageProcessor image2 = image;
            if (operation == Operation.Closing || this.operation == Operation.BottomHat) {
                image2 = image2.duplicate();
                image2.invert();
            }

            // switch depending on attribute to use
            if (attribute == Attribute.Area) {
                AreaOpeningQueue algo = new AreaOpeningQueue();
                algo.setConnectivity(this.connectivity.getNativeValue());
                DefaultAlgoListener.monitor(algo);
                result = algo.process(image2, this.minimumValue);
            } else {
                BoxDiagonalOpeningQueue algo = new BoxDiagonalOpeningQueue();
                algo.setConnectivity(this.connectivity.getNativeValue());
                DefaultAlgoListener.monitor(algo);
                result = algo.process(image2, this.minimumValue);
            }

            // For top-hat and bottom-hat, we consider difference with original image
            if (this.operation == Operation.TopHat ||
                    this.operation == Operation.BottomHat) {
                double maxDiff = 0;
                for (int i = 0; i < image.getPixelCount(); i++) {
                    float diff = Math.abs(result.getf(i) - image2.getf(i));
                    result.setf(i, diff);
                    maxDiff = Math.max(diff, maxDiff);
                }

                result.setMinAndMax(0, maxDiff);
            }

            // For closing, invert back the result
            else if (this.operation == Operation.Closing) {
                result.invert();
            }

            return result;
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImg), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Operation", description = "The operation that should be applied")
    @JIPipeParameter("operation")
    public Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    @SetJIPipeDocumentation(name = "Attribute", description = "The attribute that is used for filtering")
    @JIPipeParameter("attribute")
    public Attribute getAttribute() {
        return attribute;
    }

    @JIPipeParameter("attribute")
    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    @SetJIPipeDocumentation(name = "Minimum value", description = "The minimum attribute value")
    @JIPipeParameter("minimum-value")
    public int getMinimumValue() {
        return minimumValue;
    }

    @JIPipeParameter("minimum-value")
    public void setMinimumValue(int minimumValue) {
        this.minimumValue = minimumValue;
    }

    @SetJIPipeDocumentation(name = "Connectivity", description = "The neighborhood connectivity")
    @JIPipeParameter("connectivity")
    public Neighborhood2D getConnectivity() {
        return connectivity;
    }

    @JIPipeParameter("connectivity")
    public void setConnectivity(Neighborhood2D connectivity) {
        this.connectivity = connectivity;
    }

    public enum Operation {
        Opening,
        Closing,
        TopHat,
        BottomHat;


        @Override
        public String toString() {
            switch (this) {
                case TopHat:
                    return "Top hat";
                case BottomHat:
                    return "Bottom hat";
                default:
                    return super.toString();
            }
        }
    }

    public enum Attribute {
        Area,
        BoxDiagonal;


        @Override
        public String toString() {
            if (this == BoxDiagonal) {
                return "Box diagonal";
            }
            return super.toString();
        }
    }
}
