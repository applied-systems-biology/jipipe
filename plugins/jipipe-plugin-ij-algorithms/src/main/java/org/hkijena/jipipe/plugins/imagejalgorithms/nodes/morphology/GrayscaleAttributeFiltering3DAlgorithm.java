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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.morphology;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.morphology.AttributeFiltering;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.FilteringFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;

@SetJIPipeDocumentation(name = "Grayscale attribute filtering 3D", description = "Grayscale attribute filtering for 3D images: removes components by an attribute such as size rather than by intensity, often used to clean-up segmentation results. For grayscale input, each gray level is treated as a binary image, attribute opening is applied per level, and the results are unioned, so bright structures below the attribute threshold disappear. Closing and White/Black top-hat variants are available. This algorithm groups the incoming data by annotations.")
@AddJIPipeCitation("More information here: https://imagej.net/plugins/morpholibj#attribute-filtering")
@ConfigureJIPipeNode(function = FilteringFunction.class, framework = ImageJ1Framework.class, spatialDimensions = {SPATIAL_3D})
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = iterationStep.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleData.class, progressInfo).getImage();
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

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(resultPlus), progressInfo);
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

    @SetJIPipeDocumentation(name = "Min Voxel Number", description = "The minimum number of voxels")
    @JIPipeParameter("minimum-value")
    public int getMinVoxelNumber() {
        return minVoxelNumber;
    }

    @JIPipeParameter("minimum-value")
    public void setMinVoxelNumber(int minVoxelNumber) {
        this.minVoxelNumber = minVoxelNumber;
    }

    @SetJIPipeDocumentation(name = "Connectivity", description = "The neighborhood connectivity")
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
