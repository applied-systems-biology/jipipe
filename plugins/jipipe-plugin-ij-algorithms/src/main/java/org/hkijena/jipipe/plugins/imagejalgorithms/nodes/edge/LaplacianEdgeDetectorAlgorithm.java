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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.edge;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import ij.process.ImageProcessor;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Laplacian operator 2D", description = "Applies a Laplacian operator. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Edges", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Output", create = true)
public class LaplacianEdgeDetectorAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public static final float[] KERNEL_4;
    public static final float[] KERNEL_4_INV;
    public static final float[] KERNEL_8;
    public static final float[] KERNEL_8_INV;

    static {
        KERNEL_4_INV = new float[]{
                0, -1, 0,
                -1, 4, -1,
                0, -1, 0
        };
        KERNEL_4 = new float[]{
                0, 1, 0,
                1, -4, 1,
                0, 1, 0
        };
        KERNEL_8_INV = new float[]{
                -1, -1, -1,
                -1, 8, -1,
                -1, -1, -1
        };
        KERNEL_8 = new float[]{
                1, 1, 1,
                1, -8, 1,
                1, 1, 1
        };
    }

    private Mode mode = Mode.Laplacian4;
    private boolean removeNegativeValues = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public LaplacianEdgeDetectorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public LaplacianEdgeDetectorAlgorithm(LaplacianEdgeDetectorAlgorithm other) {
        super(other);
        this.mode = other.mode;
        this.removeNegativeValues = other.removeNegativeValues;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo).getDuplicateImage();
        float[] kernel;
        Convolver convolver = new Convolver();

        switch (mode) {
            case Laplacian8:
                kernel = KERNEL_8;
                break;
            case Laplacian8Inverted:
                kernel = KERNEL_8_INV;
                break;
            case Laplacian4:
                kernel = KERNEL_4;
                break;
            case Laplacian4Inverted:
                kernel = KERNEL_4_INV;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        ImageJIterationUtils.forEachSlice(img, imp -> {
            convolver.convolve(imp, kernel, 3, 3);
            if (removeNegativeValues) {
                float[] pixels = (float[]) imp.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = Math.max(0, pixels[i]);
                }
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Mode", description = "The filter kernel that should be used. " +
            "The inverted kernels have a positive center value, while non-inverted kernels have " +
            "a negative value at their center.")
    @JIPipeParameter("mode")
    public Mode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @SetJIPipeDocumentation(name = "Remove negative values", description = "If enabled, negative values are set to zero")
    @JIPipeParameter("remove-negative-values")
    public boolean isRemoveNegativeValues() {
        return removeNegativeValues;
    }

    @JIPipeParameter("remove-negative-values")
    public void setRemoveNegativeValues(boolean removeNegativeValues) {
        this.removeNegativeValues = removeNegativeValues;
    }

    public enum Mode {
        Laplacian4,
        Laplacian4Inverted,
        Laplacian8,
        Laplacian8Inverted;


        @Override
        public String toString() {
            switch (this) {
                case Laplacian4:
                    return "4-connected";
                case Laplacian4Inverted:
                    return "4-connected (inverted)";
                case Laplacian8:
                    return "8-connected";
                case Laplacian8Inverted:
                    return "8-connected (inverted)";
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

}
