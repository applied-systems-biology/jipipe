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

import ij.ImagePlus;
import ij.process.ImageProcessor;
import inra.ijpb.algo.DefaultAlgoListener;
import inra.ijpb.morphology.attrfilt.AreaOpeningQueue;
import inra.ijpb.morphology.attrfilt.BoxDiagonalOpeningQueue;
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
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;

@SetJIPipeDocumentation(name = "Grayscale attribute filtering 2D", description = "Grayscale attribute filtering for 2D images: removes components by an attribute such as size rather than by intensity, often used to clean-up segmentation results. For grayscale input, each gray level is treated as a binary image, attribute opening is applied per level, and the results are unioned, so bright structures below the attribute threshold disappear. Closing and White/Black top-hat variants are available.")
@AddJIPipeCitation("More information here: https://imagej.net/plugins/morpholibj#attribute-filtering")
@ConfigureJIPipeNode(function = FilteringFunction.class, framework = ImageJ1Framework.class, spatialDimensions = {SPATIAL_2D, SPATIAL_3D})
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
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
        ImagePlus outputImg = ImageJIterationUtils.generateForEachIndexedZCTSlice(inputImg, (image, index) -> {
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
