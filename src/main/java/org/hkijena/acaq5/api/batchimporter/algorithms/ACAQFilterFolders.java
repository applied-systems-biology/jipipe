package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFoldersData;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.utils.PathFilter;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@ACAQDocumentation(name = "Filter folders", description = "Filters the input folders by their name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFoldersDataSlot.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFoldersDataSlot.class, slotName = "Filtered files", autoCreate = true)
public class ACAQFilterFolders extends ACAQSimpleAlgorithm<ACAQFoldersData, ACAQFoldersData> {

    private PathFilter filter = new PathFilter();

    public ACAQFilterFolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFilterFolders(ACAQFilterFolders other) {
        super(other);
    }

    @Override
    public void run() {
        List<Path> fileNames = getInputData().getFolderPaths();
        setOutputData(new ACAQFoldersData(fileNames.stream().filter(filter).collect(Collectors.toList())));
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
