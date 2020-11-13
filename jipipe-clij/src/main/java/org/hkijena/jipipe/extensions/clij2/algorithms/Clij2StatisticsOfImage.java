package org.hkijena.jipipe.extensions.clij2.algorithms;

import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.StatisticsOfImage;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.StatisticsOfImage}
 */
@JIPipeDocumentation(name = "CLIJ2 Statistics Of Image", description = "")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "inputImage", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "resultsTable", autoCreate = true)

public class Clij2StatisticsOfImage extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2StatisticsOfImage(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2StatisticsOfImage(Clij2StatisticsOfImage other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer inputImage = dataBatch.getInputData(getInputSlot("inputImage"), CLIJImageData.class).getImage();
        ResultsTable resultsTable = new ResultsTable();
        StatisticsOfImage.statisticsOfImage(clij2, inputImage, resultsTable);

        dataBatch.addOutputData(getOutputSlot("resultsTable"), new ResultsTableData(resultsTable));
    }

}