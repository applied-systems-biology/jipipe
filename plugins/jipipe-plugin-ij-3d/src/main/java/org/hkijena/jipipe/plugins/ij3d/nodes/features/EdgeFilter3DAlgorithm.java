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

package org.hkijena.jipipe.plugins.ij3d.nodes.features;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.CannyEdge3D;
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
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "3D edge filter", description = "3D Canny-Deriche edge detection filter. " +
        "Will compute the gradients of the image based on the Canny edge detector. ")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Edges", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Edges X", create = true, description = "Edges in the X direction")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Edges Y", create = true, description = "Edges in the Y direction")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Edges Z", create = true, description = "Edges in the Z direction")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Filters/3D-Edge-and-Symmetry-Filter/")
public class EdgeFilter3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double alpha = 0.5;

    public EdgeFilter3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public EdgeFilter3DAlgorithm(EdgeFilter3DAlgorithm other) {
        super(other);
        this.alpha = other.alpha;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        Map<ImageSliceIndex, ImageProcessor> edgeXMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> edgeYMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> edgeZMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> edgeMap = new HashMap<>();
        IJ3DUtils.forEach3DIn5DIO(inputImage, (ih, index, ctProgress) -> {
            CannyEdge3D edges = new CannyEdge3D(ih, alpha);
            ImageHandler[] gg = edges.getGradientsXYZ();
            ImageHandler ed = edges.getEdge();

            IJ3DUtils.putToMap(gg[0], index.getC(), index.getT(), edgeXMap);
            IJ3DUtils.putToMap(gg[1], index.getC(), index.getT(), edgeYMap);
            IJ3DUtils.putToMap(gg[2], index.getC(), index.getT(), edgeZMap);
            IJ3DUtils.putToMap(ed, index.getC(), index.getT(), edgeMap);

        }, progressInfo);
        ImagePlus edgeX = ImageJUtils.mergeMappedSlices(edgeXMap);
        ImagePlus edgeY = ImageJUtils.mergeMappedSlices(edgeYMap);
        ImagePlus edgeZ = ImageJUtils.mergeMappedSlices(edgeZMap);
        ImagePlus edge = ImageJUtils.mergeMappedSlices(edgeMap);
        edgeX.copyScale(inputImage);
        edgeY.copyScale(inputImage);
        edgeZ.copyScale(inputImage);
        edge.copyScale(inputImage);
        iterationStep.addOutputData("Edges", new ImagePlusGreyscaleData(edge), progressInfo);
        iterationStep.addOutputData("Edges X", new ImagePlusGreyscaleData(edgeX), progressInfo);
        iterationStep.addOutputData("Edges Y", new ImagePlusGreyscaleData(edgeY), progressInfo);
        iterationStep.addOutputData("Edges Z", new ImagePlusGreyscaleData(edgeZ), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Alpha", description = "The smoothing in canny edge detection, the smaller the value, the smoother the edges.")
    @JIPipeParameter("alpha")
    public double getAlpha() {
        return alpha;
    }

    @JIPipeParameter("alpha")
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
}
