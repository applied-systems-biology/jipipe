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

package org.hkijena.jipipe.plugins.ij3d.nodes.binary;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.regionGrowing.Watershed3DVoronoi;
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
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Voronoi 3D", description = "The Voronoi algorithm will draw lines between objects at equal distances from the boundaries of" +
        " the different objects, then compute zones around objects based on these lines. This can also be seen as the splitting of the background.\n" +
        "\n" +
        "Neighbouring objects can then be computed as objects having a line in common. ")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", create = true)
public class Voronoi3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private float maxRadius = 0;

    public Voronoi3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Voronoi3DAlgorithm(Voronoi3DAlgorithm other) {
        super(other);
        this.maxRadius = other.maxRadius;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData("Input", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();

        Map<ImageSliceIndex, ImageProcessor> labelMap = new HashMap<>();
        IJ3DUtils.forEach3DIn5DIO(inputImage, (mask3D, index, ctProgress) -> {

            Watershed3DVoronoi watershed3DVoronoi = new Watershed3DVoronoi(mask3D, maxRadius);
            ImageHandler voronoiZones = watershed3DVoronoi.getVoronoiZones(false);

            IJ3DUtils.putToMap(voronoiZones, index.getC(), index.getT(), labelMap);

        }, progressInfo);

        ImagePlus outputLabels = ImageJUtils.mergeMappedSlices(labelMap);
        outputLabels.copyScale(inputImage);
        iterationStep.addOutputData("Labels", new ImagePlusGreyscaleData(outputLabels), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Max radius", description = "The maximum radius (0 for no maximum radius)")
    @JIPipeParameter("max-radius")
    public float getMaxRadius() {
        return maxRadius;
    }

    @JIPipeParameter("max-radius")
    public void setMaxRadius(float maxRadius) {
        this.maxRadius = maxRadius;
    }
}
