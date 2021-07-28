package org.hkijena.jipipe.extensions.clij2.algorithms;

import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.PushResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.PushResultsTable}
 */
@JIPipeDocumentation(name = "CLIJ2 Push Results Table", description = "")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "buffer", autoCreate = true)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "table", autoCreate = true)

public class Clij2PushResultsTable extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2PushResultsTable(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2PushResultsTable(Clij2PushResultsTable other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ResultsTable table = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).getTable();
        ClearCLBuffer buffer = clij.create(new long[]{table.getHeadings().length, table.getCounter()}, NativeTypeEnum.Float);
        PushResultsTable.pushResultsTable(clij2, buffer, table);

        dataBatch.addOutputData(getFirstOutputSlot(), new CLIJImageData(buffer), progressInfo);
    }

}