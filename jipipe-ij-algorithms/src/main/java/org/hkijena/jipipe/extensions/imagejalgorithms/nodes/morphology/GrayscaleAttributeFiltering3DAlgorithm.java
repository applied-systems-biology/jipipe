package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.morphology;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.morphology.AttributeFiltering;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@JIPipeDocumentation(name = "Grayscale attribute filtering 3D", description = "Attribute filters aim at removing components of an image based on a certain size criterion, rather than on intensity. " +
        "The most common and useful criterion is the number of pixels/voxels (i.e., the area or volume). " +
        "For example, a morphological size opening operation with a threshold value of 20 will remove all blobs containing fewer than 20 voxels. " +
        "The length of the diagonal of the bounding box can also be of interest to discriminate elongated versus round component shapes.\n\n" +
        "<h2>Application to binary images</h2>\n\n" +
        "When applied to a binary image, attribute opening consists in identifying each connected component, computing the attribute measurement of each component, and retain only the connected components whose measurement is above a specified value. This kind of processing is often used to clean-up segmentation results.\n\n" +
        "<h2>Application to grayscale images</h2>\n\n" +
        "When applied to a grayscale image, attribute opening consists in generating a series of binary images by thresholding at each distinct gray level in the image. The binary attribute opening described above is then applied independently to each binary image and the grayscale output is computed as the union of the binary results. The final output is a grayscale image whose bright structures with the attribute below a given value have disappeared. A great advantage of this filter is that the contours of the structures area better preserved than opening with a structuring element.\n\n" +
        "As for classical morphological filters, grayscale attribute closing or tophat can be defined. Grayscale attribute closing consists in removing dark connected components whose size is smaller than a specified value. White [resp. Black] Attribute Top-Hat considers the difference of the attribute opening [resp. closing] with the original image, and can help identifying bright [resp. dark] structures with small size.")
@JIPipeCitation("More information here: https://imagej.net/plugins/morpholibj#attribute-filtering")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Morphology")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nFiltering\nGray Scale Attribute Filtering 3D")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Output", autoCreate = true)
public class GrayscaleAttributeFiltering3DAlgorithm extends JIPipeIteratingAlgorithm {

    private Operation operation = Operation.Opening;
    private Attribute attribute = Attribute.Volume;

    private int minVoxelNumber = 100;
    private Neighborhood3D connectivity = Neighborhood3D.SixConnected;

    public GrayscaleAttributeFiltering3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public GrayscaleAttributeFiltering3DAlgorithm(GrayscaleAttributeFiltering3DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.attribute = other.attribute;
        this.minVoxelNumber = other.minVoxelNumber;
        this.connectivity = other.connectivity;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = dataBatch.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        ImagePlus resultPlus;
        String newName = imagePlus.getShortTitle() + "-attrFilt";

        // Identify image to process (original, or inverted)
        ImagePlus image2 = imagePlus.duplicate();
        if (operation == Operation.Closing || operation == Operation.BottomHat) {
            IJ.run(image2, "Invert", "stack");
        }

        // apply volume opening
        final ImageStack image = image2.getStack();
        final ImageStack result = AttributeFiltering.volumeOpening(image,
                minVoxelNumber, connectivity.getNativeValue());
        resultPlus = new ImagePlus(newName, result);

        // For top-hat and bottom-hat, we consider the difference with the
        // original image
        if (operation == Operation.TopHat || operation == Operation.BottomHat) {
            for (int x = 0; x < image.getWidth(); x++)
                for (int y = 0; y < image.getHeight(); y++)
                    for (int z = 0; z < image.getSize(); z++) {
                        double diff = Math.abs(result.getVoxel(x, y, z) - image.getVoxel(x, y, z));
                        result.setVoxel(x, y, z, diff);
                    }
        }

        // For closing, invert back the result
        else if (operation == Operation.Closing) {
            IJ.run(resultPlus, "Invert", "stack");
        }

        // show result
        resultPlus.copyScale(imagePlus);
        Images3D.optimizeDisplayRange(resultPlus);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(resultPlus), progressInfo);
    }

    @JIPipeDocumentation(name = "Operation", description = "The operation that should be applied")
    @JIPipeParameter("operation")
    public Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    @JIPipeDocumentation(name = "Attribute", description = "The attribute that is used for filtering")
    @JIPipeParameter("attribute")
    public Attribute getAttribute() {
        return attribute;
    }

    @JIPipeParameter("attribute")
    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    @JIPipeDocumentation(name = "Min Voxel Number", description = "The minimum number of voxels")
    @JIPipeParameter("minimum-value")
    public int getMinVoxelNumber() {
        return minVoxelNumber;
    }

    @JIPipeParameter("minimum-value")
    public void setMinVoxelNumber(int minVoxelNumber) {
        this.minVoxelNumber = minVoxelNumber;
    }

    @JIPipeDocumentation(name = "Connectivity", description = "The neighborhood connectivity")
    @JIPipeParameter("connectivity")
    public Neighborhood3D getConnectivity() {
        return connectivity;
    }

    @JIPipeParameter("connectivity")
    public void setConnectivity(Neighborhood3D connectivity) {
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
        Volume
    }
}
