package org.hkijena.acaq5.extensions.plots;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that creates {@link PlotData} from {@link ResultsTableData}
 */
@ACAQDocumentation(name = "Plot tables", description = "Converts input data tables into plots.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Analysis, menuPath = "Plot")
@AlgorithmInputSlot(ResultsTableData.class)
@AlgorithmOutputSlot(PlotData.class)
public class PlotGeneratorAlgorithm extends ACAQAlgorithm {

    /**
     * Creates a new instance
     * @param declaration The algorithm declaration
     */
    public PlotGeneratorAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public PlotGeneratorAlgorithm(PlotGeneratorAlgorithm other) {
        super(other);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
