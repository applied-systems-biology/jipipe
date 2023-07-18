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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.generate;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.causes.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.matrix.Matrix2DFloat;

/**
 * Wrapper around {@link Convolver}
 */
@JIPipeDocumentation(name = "Image from matrix", description = "Creates an image from a matrix")
@JIPipeNode(menuPath = "Convolve", nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class ImageFromMatrix2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Matrix2DFloat matrix = new Matrix2DFloat();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ImageFromMatrix2DAlgorithm(JIPipeNodeInfo info) {
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
    public ImageFromMatrix2DAlgorithm(ImageFromMatrix2DAlgorithm other) {
        super(other);
        this.matrix = new Matrix2DFloat(other.matrix);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = IJ.createImage("Matrix", "32-bit", matrix.getColumnCount(), matrix.getRowCount(), 1);
        ImageProcessor processor = img.getProcessor();
        for (int row = 0; row < matrix.getRowCount(); row++) {
            for (int col = 0; col < matrix.getColumnCount(); col++) {
                processor.setf(col, row, (float) matrix.getValueAt(row, col));
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (matrix.getRowCount() == 0 || matrix.getColumnCount() == 0) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                            new ParameterValidationReportContext(context, this, "Matrix", "matrix"),
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

}
