package org.hkijena.jipipe.extensions.clij2.algorithms;

import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.StatisticsOfBackgroundAndLabelledPixels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.StatisticsOfBackgroundAndLabelledPixels}
 */
@JIPipeDocumentation(name = "CLIJ2 Statistics Of Background And Labelled Pixels", description = "")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "inputImage", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "inputLabelMap", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "resultsTable", autoCreate = true)

public class Clij2StatisticsOfBackgroundAndLabelledPixels extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2StatisticsOfBackgroundAndLabelledPixels(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer inputImage = dataBatch.getInputData(getInputSlot("inputImage"), CLIJImageData.class).getImage();
        ClearCLBuffer inputLabelMap = dataBatch.getInputData(getInputSlot("inputLabelMap"), CLIJImageData.class).getImage();
        ResultsTable resultsTable = new ResultsTable();
        StatisticsOfBackgroundAndLabelledPixels.statisticsOfBackgroundAndLabelledPixels(clij2, inputImage, inputLabelMap, resultsTable);

        dataBatch.addOutputData(getOutputSlot("resultsTable"), new ResultsTableData(resultsTable));
    }

}