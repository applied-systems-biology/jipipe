package org.hkijena.acaq5.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.utils.PathFilter;

@ACAQDocumentation(name = "Filter files", description = "Filters the input files by their name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFileData.class, slotName = "Filtered files", autoCreate = true)

// Traits
public class ACAQFilterFiles extends ACAQIteratingAlgorithm {

    private PathFilter filter = new PathFilter();

    public ACAQFilterFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFilterFiles(ACAQFilterFiles other) {
        super(other);
        this.filter = new PathFilter(other.filter);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFileData inputData = dataInterface.getInputData("Files");
        if (filter.test(inputData.getFilePath())) {
            dataInterface.addOutputData("Filtered files", inputData);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filter").report(filter);
    }

    @ACAQParameter("filter")
    @ACAQDocumentation(name = "Filter")
    public PathFilter getFilter() {
        return filter;
    }

    @ACAQParameter("filter")
    public void setFilter(PathFilter filter) {
        this.filter = filter;
    }
}
