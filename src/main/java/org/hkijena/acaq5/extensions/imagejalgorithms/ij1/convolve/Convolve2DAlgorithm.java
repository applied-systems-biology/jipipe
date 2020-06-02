package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.convolve;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.acaq5.extensions.parameters.collections.Matrix2DFloatParameter;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.filter.Convolver}
 */
@ACAQDocumentation(name = "Convolve 2D", description = "Applies a convolution with a user-defined filter kernel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Convolve", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class Convolve2DAlgorithm extends ImageJ1Algorithm {

    private Matrix2DFloatParameter matrix = new Matrix2DFloatParameter();

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public Convolve2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale32FData.class)
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
        this.matrix = new Matrix2DFloatParameter(other.matrix);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();

        Convolver convolver = new Convolver();
        float[] kernel = new float[matrix.getRowCount() * matrix.getColumnCount()];
        for (int row = 0; row < matrix.getRowCount(); row++) {
            for (int col = 0; col < matrix.getColumnCount(); col++) {
                kernel[row * matrix.getColumnCount() + col] = (float) matrix.getValueAt(row, col);
            }
        }

        ImageJUtils.forEachSlice(img, imp -> convolver.convolve(imp, kernel, matrix.getColumnCount(), matrix.getRowCount()));

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (matrix.getRowCount() == 0 || matrix.getColumnCount() == 0) {
            report.reportIsInvalid("No matrix provided!",
                    "The convolution matrix is empty.",
                    "Please add rows and columns to the matrix.",
                    this);
        }
    }

    @ACAQDocumentation(name = "Matrix", description = "The convolution matrix")
    @ACAQParameter("matrix")
    public Matrix2DFloatParameter getMatrix() {
        return matrix;
    }

    @ACAQParameter("matrix")
    public void setMatrix(Matrix2DFloatParameter matrix) {
        this.matrix = matrix;
    }

}
