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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.parameters.matrix.Matrix2DFloat;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.filter.Convolver}
 */
@JIPipeDocumentation(name = "Convolve 2D", description = "Applies a convolution with a user-defined filter kernel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Convolve", nodeTypeCategory = ProcessorNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class Convolve2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Matrix2DFloat matrix = new Matrix2DFloat();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public Convolve2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, null)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Convolve2DAlgorithm(Convolve2DAlgorithm other) {
        super(other);
        this.matrix = new Matrix2DFloat(other.matrix);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getDuplicateImage();

        Convolver convolver = new Convolver();
        float[] kernel = new float[matrix.getRowCount() * matrix.getColumnCount()];
        for (int row = 0; row < matrix.getRowCount(); row++) {
            for (int col = 0; col < matrix.getColumnCount(); col++) {
                kernel[row * matrix.getColumnCount() + col] = (float) matrix.getValueAt(row, col);
            }
        }

        ImageJUtils.forEachSlice(img, imp -> convolver.convolve(imp, kernel, matrix.getColumnCount(), matrix.getRowCount()));

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
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

}
