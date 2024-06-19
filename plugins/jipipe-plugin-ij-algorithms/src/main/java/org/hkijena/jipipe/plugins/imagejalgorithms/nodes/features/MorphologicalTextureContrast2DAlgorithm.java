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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Morphological Texture Contrast (MTC) 2D", description = "Calculates the Morphological Texture Contrast (MTC) of the image. " +
        "Applies the following transformation to the input image f: <code>MTC = MAX(0, Opening(r2, Closing(r1, f)) - Closing(r2, Opening(r1, f)))</code>. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeCitation("Zingman, I., Saupe, D., & Lambers, K. (2014). A morphological approach for distinguishing texture and individual features in images. Pattern Recognition Letters, 47, 129-138.")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
public class MorphologicalTextureContrast2DAlgorithm  extends JIPipeSimpleIteratingAlgorithm {

    private int radius1 = 3;
    private int radius2 = 3;
    private Strel.Shape structureElement = Strel.Shape.SQUARE;

    public MorphologicalTextureContrast2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MorphologicalTextureContrast2DAlgorithm(MorphologicalTextureContrast2DAlgorithm other) {
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
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            FloatProcessor left = (FloatProcessor) Morphology.Operation.OPENING.apply(Morphology.Operation.CLOSING.apply(ip, strelR1), strelR2);
            FloatProcessor right = (FloatProcessor) Morphology.Operation.CLOSING.apply(Morphology.Operation.OPENING.apply(ip, strelR1), strelR2);
            FloatProcessor result = new FloatProcessor(inputImage.getWidth(), inputImage.getHeight());
            float[] leftPixels = (float[]) left.getPixels();
            float[] rightPixels = (float[]) right.getPixels();
            float[] resultPixels = (float[]) result.getPixels();
            for (int i = 0; i < leftPixels.length; i++) {
                resultPixels[i] = Math.max(0, leftPixels[i] - rightPixels[i]);
            }
            processorMap.put(index, result);
        }, progressInfo);
        ImagePlus outputImage = ImageJUtils.mergeMappedSlices(processorMap);
        outputImage.copyScale(inputImage);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Radius 1", description = "The first radius. Zingman et al suggest that the first radius should be chosen according to the following constraints: " +
            "<code>maxDistDetails &lt; r1 &lt; minDistIsolatedFeatures</code>, where maxDistDetails is the maximum distance between texture details and minDistIsolatedFeatures is the minimum distance to isolated features.")
    @JIPipeParameter("radius1")
    public int getRadius1() {
        return radius1;
    }

    @JIPipeParameter("radius1")
    public void setRadius1(int radius1) {
        this.radius1 = radius1;
    }

    @SetJIPipeDocumentation(name = "Radius 2", description = "The second radius. Zingman et al suggest that the second radius should be chosen according to the following constraints: " +
            "<code>maxFeatureSize &lt; r1 &lt; minTextureRegionsSize</code>, where maxFeatureSize is the maximum size of features and minTextureRegionsSize is the minimum size of texture regions.")
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
