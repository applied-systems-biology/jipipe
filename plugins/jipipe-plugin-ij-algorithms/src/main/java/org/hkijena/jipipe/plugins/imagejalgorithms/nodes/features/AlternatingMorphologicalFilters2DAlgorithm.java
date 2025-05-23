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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.features;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Alternating Morphological Filters (AMF) 2D", description = "Applies Alternating Morphological Filters (AMF) to the image. " +
        "Applies the following transformation to the input image f: <code>MTC = MAX(0, Closing(r1, Opening(r1, Closing(r1, f))) - Opening(r2, Closing(r2, Opening(r2, f)))</code>. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeCitation("Zingman, I., Saupe, D., & Lambers, K. (2014). A morphological approach for distinguishing texture and individual features in images. Pattern Recognition Letters, 47, 129-138.")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
public class AlternatingMorphologicalFilters2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int radius1 = 3;
    private int radius2 = 3;
    private Strel.Shape structureElement = Strel.Shape.SQUARE;

    public AlternatingMorphologicalFilters2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AlternatingMorphologicalFilters2DAlgorithm(AlternatingMorphologicalFilters2DAlgorithm other) {
        super(other);
        this.radius1 = other.radius1;
        this.radius2 = other.radius2;
        this.structureElement = other.structureElement;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo).getImage();
        Map<ImageSliceIndex, ImageProcessor> processorMap = new HashMap<>();
        Strel strelR1 = structureElement.fromRadius(radius1);
        Strel strelR2 = structureElement.fromRadius(radius2);
        ImageJIterationUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            FloatProcessor left = (FloatProcessor) Morphology.Operation.CLOSING.apply(Morphology.Operation.OPENING.apply(Morphology.Operation.CLOSING.apply(ip, strelR1), strelR1), strelR1);
            FloatProcessor right = (FloatProcessor) Morphology.Operation.OPENING.apply(Morphology.Operation.CLOSING.apply(Morphology.Operation.OPENING.apply(ip, strelR1), strelR2), strelR2);
            FloatProcessor result = new FloatProcessor(inputImage.getWidth(), inputImage.getHeight());
            float[] leftPixels = (float[]) left.getPixels();
            float[] rightPixels = (float[]) right.getPixels();
            float[] resultPixels = (float[]) result.getPixels();
            for (int i = 0; i < leftPixels.length; i++) {
                resultPixels[i] = leftPixels[i] - rightPixels[i];
            }
            processorMap.put(index, result);
        }, progressInfo);
        ImagePlus outputImage = ImageJUtils.mergeMappedSlices(processorMap);
        outputImage.copyScale(inputImage);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Radius 1", description = "The first radius.")
    @JIPipeParameter("radius1")
    public int getRadius1() {
        return radius1;
    }

    @JIPipeParameter("radius1")
    public void setRadius1(int radius1) {
        this.radius1 = radius1;
    }

    @SetJIPipeDocumentation(name = "Radius 2", description = "The second radius.")
    @JIPipeParameter("radius2")
    public int getRadius2() {
        return radius2;
    }

    @JIPipeParameter("radius2")
    public void setRadius2(int radius2) {
        this.radius2 = radius2;
    }

    @SetJIPipeDocumentation(name = "Structure element", description = "The shape of the structure element (default is square)")
    @JIPipeParameter("structure-element")
    public Strel.Shape getStructureElement() {
        return structureElement;
    }

    @JIPipeParameter("structure-element")
    public void setStructureElement(Strel.Shape structureElement) {
        this.structureElement = structureElement;
    }
}
