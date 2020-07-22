package org.hkijena.jipipe.extensions.clij2.algorithms;

import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.PullToResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.PullToResultsTable}
 */
@JIPipeDocumentation(name = "CLIJ2 Pull To Results Table", description = "")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Converter)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "buffer", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "table", autoCreate = true)

public class Clij2PullToResultsTable extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2PullToResultsTable(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2PullToResultsTable(Clij2PullToResultsTable other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer buffer = dataBatch.getInputData(getInputSlot("buffer"), CLIJImageData.class).getImage();
        ResultsTable table = new ResultsTable();
        PullToResultsTable.pullToResultsTable(clij2, buffer, table);

        dataBatch.addOutputData(getOutputSlot("table"), new ResultsTableData(table));
    }

}