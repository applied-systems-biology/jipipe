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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.convolve;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.matrix.Matrix2DFloat;

/**
 * Wrapper around {@link ij.plugin.filter.Convolver}
 */
@JIPipeDocumentation(name = "Convolve 2D (Parameter)", description = "Applies a convolution with a user-defined filter kernel. The kernel is defined by a parameter." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice. For the most precise results, we recommend to convert the image to 32-bit before applying a convolution. Otherwise ImageJ will apply conversion from and to 32-bit images itself, which can have unexpected results.")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
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
            ImageJUtils.convolveSlice(convolver, kernelWidth, kernelHeight, kernel, imp);
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (matrix.getRowCount() == 0 || matrix.getColumnCount() == 0) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Matrix", "matrix"),
                    "No matrix provided!",
                    "The convolution matrix is empty.",
                    "Please add rows and columns to the matrix."));
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
