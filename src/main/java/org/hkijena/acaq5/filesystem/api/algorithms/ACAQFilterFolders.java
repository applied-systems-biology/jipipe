package org.hkijena.acaq5.filesystem.api.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFolderData;
import org.hkijena.acaq5.utils.PathFilter;

@ACAQDocumentation(name = "Filter folders", description = "Filters the input folders by their name")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.FileSystem)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQFolderData.class, slotName = "Folders", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQFolderData.class, slotName = "Filtered folders", autoCreate = true)

// Traits
@AutoTransferTraits
public class ACAQFilterFolders extends ACAQIteratingAlgorithm {

    private PathFilter filter = new PathFilter();

    public ACAQFilterFolders(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQFilterFolders(ACAQFilterFolders other) {
        super(other);
        this.filter = new PathFilter(other.filter);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFolderData inputData = dataInterface.getInputData("Folders");
        if (filter.test(inputData.getFolderPath())) {
            dataInterface.addOutputData("Filtered folders", inputData);
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
