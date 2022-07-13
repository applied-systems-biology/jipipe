/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.edge;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Laplacian operator 2D", description = "Applies a Laplacian operator. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Edges", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo).getDuplicateImage();
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

        ImageJUtils.forEachSlice(img, imp -> {
            convolver.convolve(imp, kernel, 3, 3);
            if (removeNegativeValues) {
                float[] pixels = (float[]) imp.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = Math.max(0, pixels[i]);
                }
            }
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @JIPipeDocumentation(name = "Mode", description = "The filter kernel that should be used. " +
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

    @JIPipeDocumentation(name = "Remove negative values", description = "If enabled, negative values are set to zero")
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
