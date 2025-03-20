package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.features;

import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.Shadows;
import ij.process.ImageProcessor;
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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Shadows 2D", description = "Produces a shadows effect in the given direction")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nShadows")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeCitation("See https://imagej.net/ij/docs/images/shadows.gif")
public class Shadows2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Direction direction = Direction.N;
    private int iterations = 1;

    public Shadows2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Shadows2DAlgorithm(Shadows2DAlgorithm other) {
        super(other);
        this.direction = other.direction;
        this.iterations = other.iterations;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        Shadows shadows = new Shadows();
        ImagePlus outputImage = ImageJUtils.generateForEachIndexedZCTSlice(inputImage, (ip, index) -> {
            ImageProcessor outputIp =  ip.duplicate();
            for (int i = 0; i < iterations; i++) {
               switch (direction) {
                   case N:
                       shadows.north(outputIp);
                       break;
                   case NE:
                       shadows.northeast(outputIp);
                       break;
                   case E:
                       shadows.east(outputIp);
                       break;
                   case SE:
                       shadows.southeast(outputIp);
                       break;
                   case S:
                       shadows.south(outputIp);
                       break;
                   case SW:
                       shadows.southwest(outputIp);
                       break;
                   case W:
                       shadows.west(outputIp);
                       break;
                   case NW:
                       shadows.northwest(outputIp);
                       break;
                   default:
                       throw new IllegalArgumentException("Unknown direction " + direction);
               }
            }
            return outputIp;
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Direction", description = "The direction of the shadow")
    @JIPipeParameter("direction")
    public Direction getDirection() {
        return direction;
    }

    @JIPipeParameter("direction")
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @SetJIPipeDocumentation(name = "Iterations", description = "How many times the operation is applied")
    @JIPipeParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public enum Direction {
        N,
        NE,
        E,
        SE,
        S,
        SW,
        W,
        NW
    }
}
