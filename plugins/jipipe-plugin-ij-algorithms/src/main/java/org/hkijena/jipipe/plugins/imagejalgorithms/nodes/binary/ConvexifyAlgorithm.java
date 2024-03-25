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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import inra.ijpb.measure.region2d.Convexity;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Convexify", description = "Converts a 2D mask image into its convex hull. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Binary", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nBinary Images", aliasName = "Convexify")
public class ConvexifyAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ConvexifyAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ConvexifyAlgorithm(ConvexifyAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(inputImage.getWidth(), inputImage.getHeight(), inputImage.getStackSize());
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor convexify = Convexity.convexify(ip);
            stack.setProcessor(convexify, index.zeroSliceIndexToOneStackIndex(inputImage));
        }, progressInfo);
        ImagePlus outputImage = new ImagePlus("Convexify", stack);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        outputImage.copyScale(inputImage);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(outputImage), progressInfo);
    }
}
