package org.hkijena.jipipe.extensions.clij2.algorithms;

import ij.measure.ResultsTable;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GenerateParametricImageFromResultsTableColumn;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GenerateParametricImageFromResultsTableColumn}
 */
@JIPipeDocumentation(name = "CLIJ2 Generate Parametric Image From Results Table Column", description = "Take a labelmap and a column from the results table to replace label 1 with the 1st value in the vector. " + "Note that indexing in the table column starts at zero. The results table should contain a line at the beginningrepresenting the background." + " Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "label_map", autoCreate = true)
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "table", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "parametric_image_destination", autoCreate = true)

public class Clij2GenerateParametricImageFromResultsTableColumn extends JIPipeIteratingAlgorithm {
    String columnName;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2GenerateParametricImageFromResultsTableColumn(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GenerateParametricImageFromResultsTableColumn(Clij2GenerateParametricImageFromResultsTableColumn other) {
        super(other);
        this.columnName = other.columnName;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer label_map = dataInterface.getInputData(getInputSlot("label_map"), CLIJImageData.class).getImage();
        ResultsTable table = dataInterface.getInputData(getInputSlot("table"), ResultsTableData.class).getTable();
        ClearCLBuffer parametric_image_destination = clij2.create(label_map);
        GenerateParametricImageFromResultsTableColumn.generateParametricImageFromResultsTableColumn(clij2, label_map, parametric_image_destination, table, columnName);

        dataInterface.addOutputData(getOutputSlot("parametric_image_destination"), new CLIJImageData(parametric_image_destination));
    }

    @JIPipeParameter("column-name")
    public String getColumnName() {
        return columnName;
    }

    @JIPipeParameter("column-name")
    public void setColumnName(String value) {
        this.columnName = value;
    }

}