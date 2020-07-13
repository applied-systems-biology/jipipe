package org.hkijena.jipipe.extensions.clij2.algorithms;

import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.PushResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.PushResultsTable}
 */
@JIPipeDocumentation(name = "CLIJ2 Push Results Table", description = "")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Converter)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "buffer", autoCreate = true)
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "table", autoCreate = true)

public class Clij2PushResultsTable extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2PushResultsTable(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
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
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ResultsTable table = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class).getTable();
        ClearCLBuffer buffer = clij.create(new long[]{table.getHeadings().length, table.getCounter()}, NativeTypeEnum.Float);
        PushResultsTable.pushResultsTable(clij2, buffer, table);

        dataInterface.addOutputData(getFirstOutputSlot(), new CLIJImageData(buffer));
    }

}