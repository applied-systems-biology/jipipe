package org.hkijena.jipipe.extensions.clij2.algorithms;

import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.StatisticsOfBackgroundAndLabelledPixels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.StatisticsOfBackgroundAndLabelledPixels}
 */
@JIPipeDocumentation(name = "CLIJ2 Statistics Of Background And Labelled Pixels", description = "")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Analysis, menuPath = "Statistics")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "inputImage", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "inputLabelMap", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "resultsTable", autoCreate = true)

public class Clij2StatisticsOfBackgroundAndLabelledPixels extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2StatisticsOfBackgroundAndLabelledPixels(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2StatisticsOfBackgroundAndLabelledPixels(Clij2StatisticsOfBackgroundAndLabelledPixels other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer inputImage = dataInterface.getInputData(getInputSlot("inputImage"), CLIJImageData.class).getImage();
        ClearCLBuffer inputLabelMap = dataInterface.getInputData(getInputSlot("inputLabelMap"), CLIJImageData.class).getImage();
        ResultsTable resultsTable = new ResultsTable();
        StatisticsOfBackgroundAndLabelledPixels.statisticsOfBackgroundAndLabelledPixels(clij2, inputImage, inputLabelMap, resultsTable);

        dataInterface.addOutputData(getOutputSlot("resultsTable"), new ResultsTableData(resultsTable));
    }

}