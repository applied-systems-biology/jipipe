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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convolve;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.matrix.Matrix2DFloat;

/**
 * Wrapper around {@link ij.plugin.filter.Convolver}
 */
@JIPipeDocumentation(name = "Convolve 2D (Parameter)", description = "Applies a convolution with a user-defined filter kernel. The kernel is defined by a parameter." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Convolve", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFilters", aliasName = "Convolve... (matrix parameter)")
public class ConvolveByParameter2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Matrix2DFloat matrix = new Matrix2DFloat();

    private boolean normalize = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ConvolveByParameter2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        for (int i = 0; i < 3; i++) {
            matrix.addColumn();
            matrix.addRow();
        }
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ConvolveByParameter2DAlgorithm(ConvolveByParameter2DAlgorithm other) {
        super(other);
        this.matrix = new Matrix2DFloat(other.matrix);
        this.normalize = other.normalize;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();

        Convolver convolver = new Convolver();
        int kernelWidth = matrix.getColumnCount();
        int kernelHeight = matrix.getRowCount();
        float[] kernel = new float[kernelHeight * kernelWidth];
        for (int row = 0; row < kernelHeight; row++) {
            for (int col = 0; col < kernelWidth; col++) {
                kernel[row * kernelWidth + col] = (float) matrix.getValueAt(row, col);
            }
        }
        convolver.setNormalize(normalize);
        ImageJUtils.forEachSlice(img, imp -> {
            if(imp instanceof ColorProcessor) {
                // Split into channels and convolve individually
                FloatProcessor c0 = imp.toFloat(0, null);
                FloatProcessor c1 = imp.toFloat(1, null);
                FloatProcessor c2 = imp.toFloat(2, null);
                convolver.convolve(c0, kernel, kernelWidth, kernelHeight);
                convolver.convolve(c1, kernel, kernelWidth, kernelHeight);
                convolver.convolve(c2, kernel, kernelWidth, kernelHeight);
                imp.setPixels(0, c0);
                imp.setPixels(1, c1);
                imp.setPixels(2, c2);
            }
            else {
                // Convolve directly
                convolver.convolve(imp, kernel, kernelWidth, kernelHeight);
            }
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (matrix.getRowCount() == 0 || matrix.getColumnCount() == 0) {
            report.reportIsInvalid("No matrix provided!",
                    "The convolution matrix is empty.",
                    "Please add rows and columns to the matrix.",
                    this);
        }
    }

    @JIPipeDocumentation(name = "Matrix", description = "The convolution matrix")
    @JIPipeParameter("matrix")
    public Matrix2DFloat getMatrix() {
        return matrix;
    }

    @JIPipeParameter("matrix")
    public void setMatrix(Matrix2DFloat matrix) {
        this.matrix = matrix;
    }

    @JIPipeDocumentation(name = "Normalize kernel")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

}
