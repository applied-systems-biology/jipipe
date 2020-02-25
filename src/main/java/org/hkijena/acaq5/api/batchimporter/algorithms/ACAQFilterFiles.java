package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFilesData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.utils.PathFilter;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "Filter files", description = "Filters the input files by their name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFilesDataSlot.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFilesDataSlot.class, slotName = "Filtered files", autoCreate = true)
public class ACAQFilterFiles extends ACAQSimpleAlgorithm<ACAQFilesData, ACAQFilesData> {

    private PathFilter filter = new PathFilter();

    public ACAQFilterFiles(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFilterFiles(ACAQFilterFiles other) {
        super(other);
    }

    @Override
    public void run() {
        List<Path> fileNames = getInputData().getFileNames();
        setOutputData(new ACAQFilesData(fileNames.stream().filter(filter).collect(Collectors.toList())));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

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
